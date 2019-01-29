/*
 * Copyright 2018, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

"use strict";

import {Observable, Subscription, Subject, Observer} from 'rxjs';
import {QUERY_STRATEGY} from './http-endpoint';
import {SpineError} from './errors';
import {
  Subscription as SpineSubscription,
  SubscriptionId
} from '../proto/spine/client/subscription_pb';
import {Fetch} from './client';
import {AbstractClient} from './abstract-client';
import ObjectToProto from './object-to-proto';

/**
 * Fetch implementation using `FirebaseClient` as value storage.
 *
 * @see Fetch
 * @see Client#fetchAll()
 */
class FirebaseFetch extends Fetch {

  /**
   * @param {!spine.client.Query} query a request to the read-side
   * @param {!FirebaseClient} client a client used to execute requests
   */
  constructor({of: query, using: client}) {
    super({of: query, using: client});
  }

  /**
   * @inheritDoc
   */
  oneByOne() {
    return this._fetchManyOneByOne();
  }

  /**
   * @inheritDoc
   */
  atOnce() {
    return this._fetchManyAtOnce();
  }

  /**
   * Executes a request to fetch many values from Firebase one-by-one.
   *
   * @return {Promise<Object[]>} a promise resolving an array of entities matching query,
   *                             that be rejected with an `SpineError`
   */
  _fetchManyOneByOne() {
    return Observable.create(observer => {

      let receivedCount = 0;
      let promisedCount = null;
      let dbSubscription = null;

      this._client._endpoint.query(this._query, QUERY_STRATEGY.oneByOne)
        .then(({path, count}) => {
          if (typeof count === 'undefined') {
            count = 0;
          } else if (isNaN(count)) {
            throw new SpineError('Unexpected format of `count`');
          }
          promisedCount = parseInt(count);
          return path;
        })
        .then(path => {
          if (receivedCount === promisedCount) {
            FirebaseFetch._complete(observer);
          }
          dbSubscription = this._client._firebase.onChildAdded(path, value => {
            const typeUrl = this._query.getTarget().getType();
            const message = ObjectToProto.convert(value, typeUrl);

            observer.next(message);
            receivedCount++;
            if (receivedCount === promisedCount) {
              FirebaseFetch._complete(observer, dbSubscription);
            }
          });
        })
        .catch(observer.error);

      // Returning tear down logic.
      return () => {
        if (dbSubscription) {
          dbSubscription.unsubscribe();
        }
      };
    });
  }

  /**
   * A method completing an observer unsubscribing the Firebase subscriptions
   *
   * @param {!Observer} observer an observer that resolves query values
   * @param {?Subscription} dbSubscription a Firebase subscription
   * @private
   */
  static _complete(observer, dbSubscription) {
    if (dbSubscription) {
      dbSubscription.unsubscribe();
    }
    observer.complete();
  }

  /**
   * Executes a request to fetch many values from Firebase as an array of objects.
   *
   * @return {Promise<Object[]>} a promise resolving an array of entities matching query,
   *                             that be rejected with an `SpineError`
   */
  _fetchManyAtOnce() {
    return new Promise((resolve, reject) => {
      this._client._endpoint.query(this._query, QUERY_STRATEGY.allAtOnce)
        .then(({path}) => this._client._firebase.getValues(path, values => {
          const typeUrl = this._query.getTarget().getType();
          const messages = values.map(value => ObjectToProto.convert(value, typeUrl));
          resolve(messages);
        }))
        .catch(error => reject(error));
    });
  }
}

/**
 * A subscription to entity changes on application backend.
 */
class EntitySubscription extends Subscription {

  /**
   * @param {Function} unsubscribe
   * @param {{itemAdded: Observable, itemChanged: Observable, itemRemoved: Observable}} observables
   * @param {SpineSubscription} subscription
   */
  constructor({
                unsubscribedBy: unsubscribe,
                withObservables: observables,
                forInternal: subscription
              }) {
    super(unsubscribe);
    this._observables = observables;
    this._subscription = subscription;
  }

  /**
   * An internal Spine subscription which includes the topic the updates are received for.
   *
   * @return {SpineSubscription} a `spine.client.Subscription` instance
   */
  internal() {
    return this._subscription;
  }

  /**
   * @return {String} a string value of the `internal` subscription ID.
   */
  id() {
    return this.internal().getId().getValue();
  }

  /**
   * @return {EntitySubscriptionObject} a plain object with observables and unsubscribe method
   */
  toObject() {
    return Object.assign({}, this._observables, {unsubscribe: () => this.unsubscribe()})
  }
}

/**
 * An implementation of an `AbstractClient` connecting to the application backend retrieving data
 * through Firebase.
 *
 * Orchestrates the work of the HTTP and Firebase clients and the {@link ActorRequestFactory}.
 */
export class FirebaseClient extends AbstractClient {

  /**
   * @param {!HttpEndpoint} endpoint the server endpoint to execute queries and commands
   * @param {!FirebaseDatabaseClient} firebaseDatabase the client to read the query results from
   * @param {!ActorRequestFactory} actorRequestFactory a factory to instantiate the actor requests with
   * @param {!FirebaseSubscriptionService} subscriptionService a service handling the subscriptions
   *
   * @protected use `FirebaseClient#usingFirebase()` for instantiation
   */
  constructor(endpoint, firebaseDatabase, actorRequestFactory, subscriptionService) {
    super(endpoint, actorRequestFactory);
    this._firebase = firebaseDatabase;
    this._subscriptionService = subscriptionService;
    this._subscriptionService.run();
  }

  /**
   * @inheritDoc
   * @return {Client.Fetch<T>}
   * @template <T>
   */
  _fetchOf(query) {
    // noinspection JSValidateTypes A static member class type is not resolved properly.
    return new FirebaseClient.Fetch({of: query, using: this});
  }

  /**
   * @inheritDoc
   */
  _subscribeTo(topic) {
    return new Promise((resolve, reject) => {
      const typeUrl = topic.getTarget().getType();

      this._endpoint.subscribeTo(topic)
        .then(response => {
          const path = response.id.value;

          const itemAdded = new Subject();
          const itemChanged = new Subject();
          const itemRemoved = new Subject();

          const pathSubscriptions = [
            this._firebase
                .onChildAdded(path, itemAdded.next.bind(itemAdded)),
            this._firebase
                .onChildChanged(path, itemChanged.next.bind(itemChanged)),
            this._firebase
                .onChildRemoved(path, itemRemoved.next.bind(itemRemoved))
          ];

          const internalSubscription = FirebaseClient.internalSubscription(path, topic);
          const entitySubscription = new EntitySubscription({
            unsubscribedBy: () => {
              FirebaseClient._unsubscribe(pathSubscriptions);
            },
            withObservables: {
              itemAdded: ObjectToProto.map(itemAdded.asObservable(), typeUrl),
              itemChanged: ObjectToProto.map(itemChanged.asObservable(), typeUrl),
              itemRemoved: ObjectToProto.map(itemRemoved.asObservable(), typeUrl)
            },
            forInternal: internalSubscription
          });
          resolve(entitySubscription.toObject());
          this._subscriptionService.add(entitySubscription);
        })
        .catch(reject);
    });
  }

  /**
   * Unsubscribes the provided Firebase subscriptions.
   *
   * @param {Array<Subscription>} subscriptions
   * @private
   */
  static _unsubscribe(subscriptions) {
    subscriptions.forEach(subscription => {
      if (!subscription.closed) {
        subscription.unsubscribe();
      }
    });
  }

  /**
   * Creates a `SpineSubscription` instance to communicate with Spine server.
   *
   * @param {!String} path a path to object which gets updated in Firebase
   * @param {!spine.client.Topic} topic a topic for which the Subscription gets updates
   * @return {SpineSubscription} a `SpineSubscription` instance to communicate with Spine server
   */
  static internalSubscription(path, topic) {
    const subscription = new SpineSubscription();
    const id = new SubscriptionId();
    id.setValue(path);
    subscription.setId(id);
    subscription.setTopic(topic);
    return subscription;
  }
}

/**
 * @inheritDoc
 * @type FetchClass
 */
FirebaseClient.Fetch = FirebaseFetch;
