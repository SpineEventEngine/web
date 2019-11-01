/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import {Observable, Subject, Subscription} from 'rxjs';
import {
  Subscription as SpineSubscription,
  SubscriptionId
} from '../proto/spine/client/subscription_pb';
import {AbstractClientFactory} from './client-factory';
import {AbstractClient} from './abstract-client';
import ObjectToProto from './object-to-proto';
import {HttpClient} from './http-client';
import {HttpEndpoint} from './http-endpoint';
import {FirebaseDatabaseClient} from './firebase-database-client';
import {ActorRequestFactory} from './actor-request-factory';
import {FirebaseSubscriptionService} from './firebase-subscription-service';

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
   */
  execute(query) {
    return new Promise((resolve, reject) => {
      this._endpoint.query(query)
          .then(({path}) => this._firebase.getValues(path, values => {
            const typeUrl = query.getTarget().getType();
            const messages = values.map(value => ObjectToProto.convert(value, typeUrl));
            resolve(messages);
          }))
          .catch(error => reject(error));
    });
  }

  /**
   * @inheritDoc
   */
  subscribeTo(topic) {
    return new Promise((resolve, reject) => {
      const typeUrl = topic.getTarget().getType();
      this._endpoint.subscribeTo(topic)
        .then(response => {
          const path = response.nodePath.value;

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
 * An implementation of the `AbstractClientFactory` that creates instances of `FirebaseClient`.
 */
export class FirebaseClientFactory extends AbstractClientFactory {

  /**
   * Creates a new `FirebaseClient` instance which will send the requests on behalf of the provided
   * actor to the provided endpoint, retrieving the data from the provided Firebase storage.
   *
   * Expects that given options contain backend endpoint URL, firebase Database instance and
   * the actor provider.
   *
   * @param {ClientOptions} options
   * @return {Client} a new backend client instance which will send the requests on behalf
   *                  of the provided actor to the provided endpoint, retrieving the data
   *                  from the provided Firebase storage
   * @override
   */
  static _clientFor(options) {
    const httpClient = new HttpClient(options.endpointUrl);
    const endpoint = new HttpEndpoint(httpClient);
    const firebaseDatabaseClient = new FirebaseDatabaseClient(options.firebaseDatabase);
    const requestFactory = new ActorRequestFactory(options.actorProvider);
    const subscriptionService = new FirebaseSubscriptionService(endpoint);

    return new FirebaseClient(endpoint, firebaseDatabaseClient, requestFactory, subscriptionService);
  }

  /**
   * @override
   */
  static _ensureOptionsSufficient(options) {
    super._ensureOptionsSufficient(options);
    const messageForMissing = (option) =>
        `Unable to initialize Client with Firebase storage. The ClientOptions.${option} not specified.`;
    if (!options.endpointUrl) {
      throw new Error(messageForMissing('endpointUrl'));
    }
    if (!options.firebaseDatabase) {
      throw new Error(messageForMissing('firebaseDatabase'));
    }
    if (!options.actorProvider) {
      throw new Error(messageForMissing('endpointUrl'));
    }
  }
}
