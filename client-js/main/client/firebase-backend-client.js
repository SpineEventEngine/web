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

import {Observable, Subscription} from 'rxjs';
import {HttpEndpoint, QUERY_STRATEGY} from './http-endpoint';
import {SpineError} from './errors';
import {
  Subscription as SpineSubscription,
  SubscriptionId
} from '../proto/spine/client/subscription_pb';
import {Fetch} from './backend-client';
import {AbstractBackendClient} from './abstract-backend-client';
import {HttpClient} from './http-client';
import {FirebaseClient} from './firebase-client';
import {ActorRequestFactory} from './actor-request-factory';
import {FirebaseSubscriptionService} from './firebase-subscription-service';

/**
 * Fetch implementation using `FirebaseBackendClient` as value storage.
 *
 * @see Fetch
 * @see BackendClient#fetchAll()
 */
class FirebaseFetch extends Fetch {

  /**
   * @param {!TypedQuery} query a typed query which contains runtime information about the queried entity type
   * @param {!FirebaseBackendClient} backend a Firebase backend client used to execute requests
   */
  constructor({of: query, using: backend}) {
    super({of: query, using: backend});
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

      const query = this._query.raw();
      this._backend._endpoint.query(query, QUERY_STRATEGY.oneByOne)
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
          dbSubscription = this._backend._firebase.onChildAdded(path, value => {
            const message = this._query.convert(value);
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
      const query = this._query.raw();
      this._backend._endpoint.query(query, QUERY_STRATEGY.allAtOnce)
        .then(({path}) => this._backend._firebase.getValues(path, values => {
          let messages = values.map(value => {
            const message = this._query.convert(value);
            return message;
          });
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

  constructor({
                unsubscribedBy: unsubscribe,
                withObservables: observables,
                forSubscription: subscription
              }) {
    super(unsubscribe);
    this.itemAdded = observables.add;
    this.itemChanged = observables.change;
    this.itemRemoved = observables.remove;
    this._subscription = subscription;
  }

  /**
   * An internal Spine subscription which includes the topic the updates are received for.
   *
   * @return {SpineSubscription} a `spine.client.Subscription` instane
   */
  internal() {
    return this._subscription;
  }

  /**
   * @return {String} a string value of the `internal` subscription id.
   */
  id() {
    return this.internal().getId().getValue();
  }

  /**
   * @return {EntitySubscriptionObject} a plain object with observables and unsubscribe method
   */
  toObject() {
    return {
      itemAdded: this.itemAdded,
      itemChanged: this.itemChanged,
      itemRemoved: this.itemRemoved,
      unsubscribe: () => this.unsubscribe()
    };
  }
}

/**
 * An implementation of a client connecting to the application backend retrieving data
 * through Firebase.
 *
 * To initialize a new instance do the following:
 * ```
 *  import * as protobufs from './proto/index.js';
 *
 *  const firebaseApp = Firebase.initializeApp({...Firebase options});
 *
 *  // The backend client will receive updates of the current actor through this instance
 *  const actorProvider = new ActorProvider();
 *
 *  const backendClient = FirebaseBackendClient.forProtobufTypes(protobufs)
 *                                             .usingFirebase({
 *                                               atEndpoint: 'http://example.appspot.com',
 *                                               withFirebaseStorage: firebaseApp,
 *                                               forActor: actorProvider}
 *                                             })
 * ```
 *
 * Orchestrates the work of the HTTP and Firebase clients and the {@link ActorRequestFactory}.
 */
export class FirebaseBackendClient extends AbstractBackendClient {

  /**
   * @param {!HttpEndpoint} endpoint the server endpoint to execute queries and commands
   * @param {!FirebaseClient} firebaseClient the client to read the query results from
   * @param {!ActorRequestFactory} actorRequestFactory a factory to instantiate the actor requests with
   * @param {!FirebaseSubscriptionService} subscriptionService a service handling the subscriptions
   *
   * @protected use `FirebaseBackendClient#usingFirebase()` for instantiation
   */
  constructor(endpoint, firebaseClient, actorRequestFactory, subscriptionService) {
    super(endpoint, actorRequestFactory);
    this._firebase = firebaseClient;
    this._subscriptionService = subscriptionService;
    this._subscriptionService.run();
  }

  /**
   * A static factory method that creates a new `BackendClient` instance using Firebase as
   * underlying implementation.
   *
   * @param {!string} atEndpoint a Spine web backend endpoint URL
   * @param {!firebase.app.App} withFirebaseStorage
   *        a Firebase Application that will be used to retrieve data from
   * @param {!ActorProvider} forActor a provider of the user interacting with Spine
   * @return {BackendClient} a new backend client instance which will send the requests on behalf
   *                          of the provided actor to the provided endpoint, retrieving the data
   *                          from the provided Firebase storage
   */
  static usingFirebase({atEndpoint: endpointUrl, withFirebaseStorage: firebaseApp, forActor: actorProvider}) {
    const httpClient = new HttpClient(endpointUrl);
    const endpoint = new HttpEndpoint(httpClient);
    const firebaseClient = new FirebaseClient(firebaseApp);
    const requestFactory = new ActorRequestFactory(actorProvider);
    const subscriptionService = new FirebaseSubscriptionService(endpoint);

    return new FirebaseBackendClient(endpoint, firebaseClient, requestFactory, subscriptionService);
  }

  /**
   * @inheritDoc
   * @return {BackendClient.Fetch<T>}
   * @template <T>
   */
  _fetchOf(query) {
    // noinspection JSValidateTypes A static member class type is not resolved properly.
    return new FirebaseBackendClient.Fetch({of: query, using: this});
  }

  /**
   * @inheritDoc
   */
  _subscribeToTopic(topic) {
    return new Promise((resolve, reject) => {
      const spineTopic = topic.raw();
      this._endpoint.subscribeTo(spineTopic)
        .then(subscription => {
          const path = subscription.id.value;
          const subscriptions = {add: null, remove: null, change: null};
          const add = Observable.create(observer => {
            subscriptions.add = this._firebase.onChildAdded(path, value => {
              const message = topic.convert(value);
              observer.next(message);
            });
          });
          const change = Observable.create(observer => {
            subscriptions.change = this._firebase.onChildChanged(path, value => {
              const message = topic.convert(value);
              observer.next(message);
            });
          });
          const remove = Observable.create(observer => {
            subscriptions.remove = this._firebase.onChildRemoved(path, value => {
              const message = topic.convert(value);
              observer.next(message);
            });
          });
          const subscriptionProto = FirebaseBackendClient.subscriptionProto(path, topic);
          const entitySubscription = new EntitySubscription({
            unsubscribedBy: () => {
              FirebaseBackendClient._tearDownSubscriptions(subscriptions);
            },
            withObservables: {add, change, remove},
            forSubscription: subscriptionProto
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
   * @param {{add: Subscription, remove: Subscription, change: Subscription}} subscriptions
   * @private
   */
  static _tearDownSubscriptions(subscriptions) {
    if (!subscriptions.add.closed) {
      subscriptions.add.unsubscribe();
    }
    if (!subscriptions.remove.closed) {
      subscriptions.remove.unsubscribe();
    }
    if (!subscriptions.change.closed) {
      subscriptions.change.unsubscribe();
    }
  }

  /**
   * Creates a Protobuf `Subscription` instance to communicate with Spine server.
   *
   * @param {String} path a path to object which gets updated in Firebase
   * @param {TypedTopic} topic a topic for which the Subscription gets updates
   */
  static subscriptionProto(path, topic) {
    const subscription = new SpineSubscription();
    const id = new SubscriptionId();
    id.setValue(path);
    subscription.setId(id);
    const spineTopic = topic.raw();
    subscription.setTopic(spineTopic);
    return subscription;
  }
}

/**
 * @inheritDoc
 * @type FetchClass
 */
FirebaseBackendClient.Fetch = FirebaseFetch;
