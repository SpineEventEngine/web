/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
  Subscription as SubscriptionObject,
  SubscriptionId
} from '../proto/spine/client/subscription_pb';
import {ActorRequestFactory} from './actor-request-factory';
import {AbstractClientFactory} from './client-factory';
import {CommandingClient} from "./commanding-client";
import {CompositeClient} from "./composite-client";
import {HttpEndpoint} from './http-endpoint';
import {FirebaseDatabaseClient} from './firebase-database-client';
import {FirebaseSubscriptionService} from './firebase-subscription-service';
import ObjectToProto from './object-to-proto';
import {QueryingClient} from "./querying-client";
import {SubscribingClient} from "./subscribing-client";

/**
 * An abstract base for subscription objects.
 *
 * @abstract
 */
class SpineSubscription extends Subscription {

  /**
   * @param {Function} unsubscribe the callbacks that allows to cancel the subscription
   * @param {SubscriptionObject} subscription the wrapped subscription object
   *
   * @protected
   */
  constructor(unsubscribe, subscription) {
    super(unsubscribe);
    this._subscription = subscription;
  }

  /**
   * An internal Spine subscription which includes the topic the updates are received for.
   *
   * @return {SubscriptionObject} a `spine.client.Subscription` instance
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
}

/**
 * A subscription to entity changes on application backend.
 */
class EntitySubscription extends SpineSubscription {

  /**
   * @param {Function} unsubscribe the callback that allows to cancel the subscription
   * @param {{itemAdded: Observable, itemChanged: Observable, itemRemoved: Observable}} observables
   *        the observables for entity change
   * @param {SubscriptionObject} subscription the wrapped subscription object
   */
  constructor({
                unsubscribedBy: unsubscribe,
                withObservables: observables,
                forInternal: subscription
              }) {
    super(unsubscribe, subscription);
    this._observables = observables;
  }

  /**
   * @return {EntitySubscriptionObject} a plain object with observables and unsubscribe method
   */
  toObject() {
    return Object.assign({}, this._observables, {unsubscribe: () => this.unsubscribe()});
  }
}

/**
 * A subscription to events that occur in the system.
 */
class EventSubscription extends SpineSubscription {

  /**
   * @param {Function} unsubscribe the callbacks that allows to cancel the subscription
   * @param {Observable} eventEmitted the observable for the emitted events
   * @param {SubscriptionObject} subscription the wrapped subscription object
   */
  constructor({
                unsubscribedBy: unsubscribe,
                withObservable: observable,
                forInternal: subscription
              }) {
    super(unsubscribe, subscription);
    this._observable = observable;
  }

  /**
   * @return {EventSubscriptionObject} a plain object with observables and unsubscribe method
   */
  toObject() {
    return Object.assign(
        {}, {eventEmitted: this._observable}, {unsubscribe: () => this.unsubscribe()}
    );
  }
}

class FirebaseQueryingClient extends QueryingClient {

  /**
   * Creates an instance of the client.
   *
   * @param {!HttpEndpoint} endpoint the server endpoint to execute queries and commands
   * @param {!FirebaseDatabaseClient} firebaseDatabase the client to read the query results from
   * @param {!ActorRequestFactory} actorRequestFactory a factory to instantiate the actor requests with
   */
  constructor(endpoint, firebaseDatabase, actorRequestFactory) {
    super(actorRequestFactory);
    this._endpoint = endpoint;
    this._firebase = firebaseDatabase;
  }

  read(query) {
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
}

const EVENT_TYPE_URL = 'type.spine.io/spine.core.Event';

/**
 * A {@link SubscribingClient} which receives entity state updates and events from
 * a Firebase Realtime Database.
 */
class FirebaseSubscribingClient extends SubscribingClient {

  /**
   * Creates an instance of the client.
   *
   * @param {!HttpEndpoint} endpoint
   *  the server endpoint to execute queries and commands
   * @param {!FirebaseDatabaseClient} firebaseDatabase
   *  the client to read the query results from
   * @param {!ActorRequestFactory} actorRequestFactory
   *  a factory to instantiate the actor requests with
   * @param {!FirebaseSubscriptionService} subscriptionService
   *  a service handling the subscriptions
   */
  constructor(endpoint, firebaseDatabase, actorRequestFactory, subscriptionService) {
    super(actorRequestFactory);
    this._endpoint = endpoint;
    this._firebase = firebaseDatabase;
    this._subscriptionService = subscriptionService;
  }

  /**
   * @inheritDoc
   */
  subscribe(topic) {
    return this._doSubscribe(topic, this._entitySubscription);
  }

  /**
   * @inheritDoc
   */
  subscribeToEvents(topic) {
    return this._doSubscribe(topic, this._eventSubscription);
  }

  /**
   * @private
   */
  _doSubscribe(topic, createSubscriptionFn) {
    return new Promise((resolve, reject) => {
      this._endpoint.subscribeTo(topic)
          .then(response => {
            const path = response.nodePath.value;
            const internalSubscription =
                FirebaseSubscribingClient.internalSubscription(path, topic);

            const subscription = createSubscriptionFn.call(this, path, internalSubscription);

            resolve(subscription.toObject());
            this._subscriptionService.add(subscription);
          })
          .catch(reject);
    });
  }

  /**
   * @private
   */
  _entitySubscription(path, subscription) {
    const itemAdded = new Subject();
    const itemChanged = new Subject();
    const itemRemoved = new Subject();

    const pathSubscriptions = [
      this._firebase.onChildAdded(path, itemAdded),
      this._firebase.onChildChanged(path, itemChanged),
      this._firebase.onChildRemoved(path, itemRemoved)
    ];

    const typeUrl = subscription.getTopic().getTarget().getType();

    return new EntitySubscription({
      unsubscribedBy: () => {
        FirebaseSubscribingClient._unsubscribe(pathSubscriptions);
        // TODO:alex.tymchenko:2023-01-17: find out how to report `Promise` errors, and where.
        this._subscriptionService.cancelSubscription(subscription)
            .then(
                (result) => {
                  console.log("Subscription successfully cancelled: " + JSON.stringify(result))
                },
                () => console.warn("Error sending the subscription" +
                    " cancellation request to the server-side.")
            );
      },
      withObservables: {
        itemAdded: ObjectToProto.map(itemAdded.asObservable(), typeUrl),
        itemChanged: ObjectToProto.map(itemChanged.asObservable(), typeUrl),
        itemRemoved: ObjectToProto.map(itemRemoved.asObservable(), typeUrl)
      },
      forInternal: subscription
    });
  }

  /**
   * @private
   */
  _eventSubscription(path, subscription) {
    const itemAdded = new Subject();
    const pathSubscription = this._firebase.onChildAdded(path, itemAdded);

    return new EventSubscription({
      unsubscribedBy: () => {
        FirebaseSubscribingClient._unsubscribe([pathSubscription]);
      },
      withObservable: ObjectToProto.map(itemAdded.asObservable(), EVENT_TYPE_URL),
      forInternal: subscription
    });
  }

  /**
   * @override
   */
  cancelAllSubscriptions() {
    this._subscriptionService.cancelAllSubscriptions();
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
   * Creates a `SubscriptionObject` instance to communicate with Spine server.
   *
   * @param {!String} path a path to object which gets updated in Firebase
   * @param {!spine.client.Topic} topic a topic for which the Subscription gets updates
   * @return {SubscriptionObject} a `SubscriptionObject` instance to communicate with Spine server
   */
  static internalSubscription(path, topic) {
    const subscription = new SubscriptionObject();
    const id = new SubscriptionId();
    id.setValue(path);
    subscription.setId(id);
    subscription.setTopic(topic);
    return subscription;
  }
}

/**
 * An implementation of the `AbstractClientFactory` that creates instances of client which exchanges
 * data with the server via Firebase Realtime Database.
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
    const httpClient = this._createHttpClient(options);
    const httpResponseHandler = this._createHttpResponseHandler(options);
    const endpoint = new HttpEndpoint(httpClient, httpResponseHandler, options.routing);
    const firebaseDatabaseClient = new FirebaseDatabaseClient(options.firebaseDatabase);
    const requestFactory = ActorRequestFactory.create(options);
    const subscriptionService =
        new FirebaseSubscriptionService(endpoint, options.subscriptionKeepUpInterval);

    const querying = new FirebaseQueryingClient(endpoint, firebaseDatabaseClient, requestFactory);
    const subscribing = new FirebaseSubscribingClient(endpoint,
                                                      firebaseDatabaseClient,
                                                      requestFactory,
                                                      subscriptionService);
    const commanding = new CommandingClient(endpoint, requestFactory);
    return new CompositeClient(querying, subscribing, commanding);
  }

  static createQuerying(options) {
    const httpClient = this._createHttpClient(options);
    const httpResponseHandler = this._createHttpResponseHandler(options);
    const endpoint = new HttpEndpoint(httpClient, httpResponseHandler, options.routing);
    const firebaseDatabaseClient = new FirebaseDatabaseClient(options.firebaseDatabase);
    const requestFactory = ActorRequestFactory.create(options);

    return new FirebaseQueryingClient(endpoint, firebaseDatabaseClient, requestFactory);
  }

  static createSubscribing(options) {
    const httpClient = this._createHttpClient(options);
    const httpResponseHandler = this._createHttpResponseHandler(options);
    const endpoint = new HttpEndpoint(httpClient, httpResponseHandler, options.routing);
    const firebaseDatabaseClient = new FirebaseDatabaseClient(options.firebaseDatabase);
    const requestFactory = ActorRequestFactory.create(options);
    const subscriptionService =
        new FirebaseSubscriptionService(endpoint, options.subscriptionKeepUpInterval);

    return new FirebaseSubscribingClient(endpoint,
                                         firebaseDatabaseClient,
                                         requestFactory,
                                         subscriptionService);
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
      throw new Error(messageForMissing('actorProvider'));
    }
  }
}
