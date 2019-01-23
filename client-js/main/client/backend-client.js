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
import {
  TypedMessage,
  Type
} from './typed-message';
import {HttpEndpoint, QUERY_STRATEGY} from './http-endpoint';
import {SpineError, CommandHandlingError, CommandValidationError} from './errors';
import {HttpClient} from './http-client';
import {FirebaseClient} from './firebase-client';
import {ActorRequestFactory} from './actor-request-factory';
import {FirebaseSubscriptionService} from './firebase-subscription-service';
import KnownTypes from './known-types';
import TypeParsers from './parser/type-parsers';
import {
  Subscription as SpineSubscription,
  SubscriptionId
} from '../proto/spine/client/subscription_pb';
import {Status} from '../proto/spine/core/response_pb';

/**
 * A utility which converts the JS object to its Protobuf counterpart.
 */
class ObjectToProto {

  /**
   * Converts the object to the corresponding Protobuf message.
   *
   * The input object is supposed to be a Protobuf message representation, i.e. all its attributes should correspond to
   * the fields of the specified message type.
   *
   * @param {Object} object an object to convert
   * @param {Type} type a type of the corresponding Protobuf message
   */
  static convert(object, type) {
    const typeUrl = type.url().value();
    const parser = TypeParsers.parserFor(typeUrl);
    const proto = parser.fromObject(object);
    return proto;
  }
}

/**
 * Matches the static information about the query with the queried entity type gathered at runtime.
 */
class TypedQuery {

  /**
   * @param {!spine.client.Query} query an underlying proto message representing the query
   * @param {!Type} type a type of the entity this query is targeted on
   */
  constructor(query, type) {
    this._query = query;
    this._type = type;
  }

  /**
   * Retrieves the underlying proto message.
   */
  raw() {
    return this._query;
  }

  /**
   * Converts a query response sent by the server to the corresponding proto message.
   *
   * @param {!Object} response an object representing the response for the query
   */
  convert(response) {
    return ObjectToProto.convert(response, this._type);
  }
}

/**
 * Matches the static information about the topic with the subscribed entity type gathered at runtime.
 */
class TypedTopic {

  /**
   * @param {!spine.client.Topic} topic an underlying proto message representing the topic
   * @param {!Type} type a type of the subscription target
   */
  constructor(topic, type) {
    this._topic = topic;
    this._type = type;
  }

  /**
   * Retrieves the underlying proto message.
   */
  raw() {
    return this._topic;
  }

  /**
   * Converts an object which represents the observed entity update to the corresponding proto message.
   *
   * @param {!Object} update an object representing the entity update
   */
  convert(update) {
    return ObjectToProto.convert(update, this._type);
  }
}

/**
 * An abstract Fetch that can fetch the data of a provided query in one of two ways
 * (one-by-one or all-at-once) using the provided backend.
 *
 * Fetch is a static member of the `BackendClient`.
 *
 * @template <T>
 * @abstract
 */
class Fetch {

  /**
   * @param {!TypedQuery} query a typed query which contains runtime information about the queried entity type
   * @param {!BackendClient} backend the backend which is used to fetch the query results
   */
  constructor({of: query, using: backend}) {
    this._query = query;
    this._backend = backend;
  }

  /**
   * Fetches entities one-by-one using an observable. Provides each entity as a new value for
   * the subscribed Observer.
   *
   * This method is suitable for big collections of data where ordering is not essential.
   *
   * @example
   * // To query all entities of developer-defined Task type one-by-one:
   * fetchAll({ofType: taskType}).oneByOne().subscribe({
   *   next(task) { ... },
   *   error(error) { ... },
   *   complete() { ... }
   * })
   *
   * @return {Observable<Object, SpineError>} an observable retrieving values one at a time.
   * @abstract
   */
  oneByOne() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Fetches all query results at once fulfilling a promise with an array of entities.
   *
   * @example
   * // To query all entities of developer-defined Task type at once:
   * fetchAll({ofType: taskType}).atOnce().then(tasks => { ... })
   *
   * @return {Promise<Object[]>} a promise to be fulfilled with an array of entities matching query
   *                             or to be rejected with a `SpineError`
   * @abstract
   */
  atOnce() {
    throw new Error('Not implemented in abstract base.');
  }
}

/**
 * The callback that doesn't accept arguments.
 * @callback voidCallback
 */

/**
 * The callback that accepts single argument.
 *
 * @callback consumerCallback
 * @param {T} the value the callback function accepts
 *
 * @template <T>
 */

/**
 * @typedef {Object} EntitySubscriptionObject
 *
 * @property {Observable<T>} itemAdded
 * @property {Observable<T>} itemChanged
 * @property {Observable<T>} itemRemoved
 * @property {voidCallback} unsubscribe a method to be called to cancel the subscription, stopping 
 *                                      the subscribers from receiving new entities
 *
 * @template <T>
 */

/**
 * An abstract client for Spine application backend. This is a single channel for client-server
 * communication in a Spine-based browser application.
 *
 * Backend client defines operations that client is able to perform (`.fetchAll(...)`,
 * `.sendCommand(...)`, etc.), also providing factory methods for creating Backend Client
 * instances (`.usingFirebase(...)`).
 *
 * Protobuf types that will be used with the client should be registered via `registerTypes(...)`.
 *
 * @abstract
 */
export class BackendClient {

  /**
   * @param {!Endpoint} endpoint an endpoint to send requests to
   * @param {!ActorRequestFactory} actorRequestFactory
   *        a request factory to build requests to Spine server
   */
  constructor(endpoint, actorRequestFactory) {
    this._endpoint = endpoint;
    this._requestFactory = actorRequestFactory;
  }

  /**
   * Defines a fetch query of all entities matching the filters provided as arguments.
   * This fetch is executed later upon calling the corresponding `.oneByOne()` and
   * `.atOnce()` methods.
   *
   * `fetchAll(...).oneByOne()` queries the entities returning them in asynchronous manner using
   * an observable. A subscriber is added to an observable to process each next entity or handle
   * the error during the operation.
   *
   * `fetchAll(...).atOnce()` queries all the entities at once fulfilling a returned promise
   * with an array of objects.
   *
   * @example
   * // Fetch all entities of a developer-defined Task type one-by-one using an observable.
   * fetchAll({ofType: taskType}).oneByOne().subscribe({
   *   next(task) { ... },
   *   error(error) { ... },
   *   complete() { ... }
   * })
   * @example
   * // Fetch all entities of a developer-defined Task type at once using a Promise.
   * fetchAll({ofType: taskType}).atOnce().then(tasks => { ... })
   *
   * @param {!Type<T>} ofType a type of the entities to be queried
   * @return {BackendClient.Fetch<T>} a fetch object allowing to specify additional remote
   *                                call parameters and executed the query.
   *
   * @template <T>
   */
  fetchAll({ofType: type}) {
    const query = this._requestFactory.query().select(type).build();
    const typedQuery = new TypedQuery(query, type);
    return this._fetchOf(typedQuery);
  }

  /**
   * The callback that doesn't accept arguments.
   * @callback voidCallback
   */

    /**
     * The callback that accepts single argument.
     *
     * @callback consumerCallback
     * @param {T} the value the callback function accepts
     *
     * @template <T>
     */

  /**
   * Fetches a single entity of the given type.
   *
   * @param {!Type<T>} type a type URL of the target entity
   * @param {!Message} id an ID of the target entity
   * @param {!Function} dataCallback
   *        a callback receiving a single data item as a Protobuf message of a given type; receives `null` if an
   *        entity with a given ID is missing
   * @param {?consumerCallback<SpineError>} errorCallback
   *        a callback receiving an error
   *
   * @template <T>
   */
  fetchById(type, id, dataCallback, errorCallback) {
    const typedId = TypedMessage.of(id);
    const query = this._requestFactory.query().select(type).byIds([typedId]).build();
    const typedQuery = new TypedQuery(query, type);

    let itemReceived = false;

    const observer = {
      next: item => {
        itemReceived = true;
          dataCallback(item);
      },
      complete: () => {
        if (!itemReceived) {
          dataCallback(null);
        }
      }
    };

    if (errorCallback) {
      observer.error = errorCallback;
    }
    this._fetchOf(typedQuery).oneByOne().subscribe(observer);
  }

  /**
   * Sends the provided command to the server.
   *
   * After sending the command to the server the following scenarios are possible:
   *
   *  - the `acknowledgedCallback` is called if the command is acknowledged for further processing
   *  - the `errorCallback` is called if sending of the command failed
   *
   * Invocation of the `acknowledgedCallback` and the `errorCallback` are mutually exclusive.
   *
   * If the command sending fails, the respective error is passed to the `errorCallback`. This error is
   * always the type of `CommandHandlingError`. Its cause can be retrieved by `getCause()` method and can
   * be represented with the following types of errors:
   *
   *  - `ConnectionError`  – if the connection error occurs;
   *  - `ClientError`      – if the server responds with `4xx` HTTP status code;
   *  - `ServerError`      – if the server responds with `5xx` HTTP status code;
   *  - `spine.base.Error` – if the command message can't be processed by the server;
   *  - `SpineError`       – if parsing of the response fails;
   *
   * If the command sending fails due to a command validation error, an error passed to the
   * `errorCallback` is the type of `CommandValidationError` (inherited from `CommandHandlingError`).
   * The validation error can be retrieved by `validationError()` method.
   *
   * The occurrence of an error does not guarantee that the command is not accepted by the server
   * for further processing. To verify this, call the error `assuresCommandNeglected()` method.
   *
   * @param {!Message} commandMessage a Protobuf message representing the comand
   * @param {!voidCallback} acknowledgedCallback
   *        a no-argument callback invoked if the command is acknowledged
   * @param {?consumerCallback<CommandHandlingError>} errorCallback
   *        a callback receiving the errors executed if an error occurred when sending command
   * @param {?consumerCallback<Rejection>} rejectionCallback
   *        a callback executed if the command was rejected by Spine server
   * @see CommandHandlingError
   * @see CommandValidationError
   */
  sendCommand(commandMessage, acknowledgedCallback, errorCallback, rejectionCallback) {
    const command = this._requestFactory.command().create(commandMessage);
    this._endpoint.command(command)
      .then(ack => {
        const responseStatus = ack.status;
        const responseStatusProto = ObjectToProto.convert(responseStatus, Type.forClass(Status));
        const responseStatusCase = responseStatusProto.getStatusCase();

        switch (responseStatusCase) {
          case Status.StatusCase.OK:
            acknowledgedCallback();
            break;
          case Status.StatusCase.ERROR:
            const error = responseStatusProto.getError();
            const message = error.getMessage();
            errorCallback(error.hasValidationError()
                ? new CommandValidationError(message, error)
                : new CommandHandlingError(message, error));
            break;
          case Status.StatusCase.REJECTION:
            rejectionCallback(responseStatusProto.getRejection());
            break;
          default:
            errorCallback(new SpineError(`Unknown response status case ${responseStatusCase}`));
        }
      })
      .catch(error => errorCallback(new CommandHandlingError(error.message, error)));
  }

  /**
   * Subscribes to entity changes on the backend, providing the changes via `itemAdded`,
   * `itemChanged`, and `itemRemoved` observers.
   *
   * The changes can be handled for a one or many entities by specifying the entity type
   * and the ids.
   *
   * The entities that already exist will be initially passed to the `itemAdded` observer. 
   *
   * @param {!Type} ofType a type URL of entities to observe changes
   * @param {?Message[]} byIds an array of ids of entities to observe changes
   * @param {?Message} byId an id of a single entity to observe changes
   * @return {Promise<EntitySubscriptionObject>} a promise of means to observe the changes 
   *                                             and unsubscribe from the updated 
   */
  subscribeToEntities({ofType: type, byIds: ids, byId: id}) {
    if (typeof ids !== 'undefined' && typeof id !== 'undefined') {
      throw new Error('You can specify only one of ids or id as a parameter to subscribeToEntities');
    }
    if (typeof id !== 'undefined') {
      ids = [id];
    }
    let topic;
    if (ids) {
      const typedIds = ids.map(TypedMessage.of);
      topic = this._requestFactory.topic().all({of: type, withIds: typedIds});
    } else {
      topic = this._requestFactory.topic().all({of: type});
    }
    const typedTopic = new TypedTopic(topic, type);
    return this._subscribeToTopic(typedTopic);
  }

  /**
   * A static factory method that creates a new `BackendClient` instance using Firebase as
   * underlying implementation.
   *
   * @param {!string} atEndpoint a Spine web backend endpoint URL
   * @param {!firebase.app.App} withFirebaseStorage
   *        a Firebase Application that will be used to retrieve data from
   * @param {!string} forActor an id of the user interacting with Spine
   * @return {BackendClient} a new backend client instance which will send the requests on behalf
   *                          of the provided actor to the provided endpoint, retrieving the data
   *                          from the provided Firebase storage
   */
  static usingFirebase({atEndpoint: endpointUrl, withFirebaseStorage: firebaseApp, forActor: actor}) {
    const httpClient = new HttpClient(endpointUrl);
    const endpoint = new HttpEndpoint(httpClient);
    const firebaseClient = new FirebaseClient(firebaseApp);
    const requestFactory = new ActorRequestFactory(actor);
    const subscriptionService = new FirebaseSubscriptionService(endpoint);

    return new FirebaseBackendClient(endpoint, firebaseClient, requestFactory, subscriptionService);
  }


  /**
   * Registers all Protobuf types provided by the specified modules.
   *
   * <p>After the registration, the types can be used and parsed correctly.
   *
   * @example
   * import * as protobufs from './proto/index.js';
   * BackendClient.registerTypes(protobufs);
   *
   * @param protoIndexFiles the index.js files generated by
   * {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
   */
  static registerTypes(...protoIndexFiles) {
    for (let indexFile of protoIndexFiles) {
      this._registerTypes(indexFile);
    }
  }

  static _registerTypes(indexFile) {
    for (let [typeUrl, type] of indexFile.types) {
      KnownTypes.register(type, typeUrl);
    }
    for (let [typeUrl, parserType] of indexFile.parsers) {
      TypeParsers.register(new parserType(), typeUrl);
    }
  }

  /**
   * Creates a new Fetch object specifying the target of fetch and its parameters.
   *
   * @param {!TypedQuery} query a typed query which contains runtime information about the queried entity type
   * @return {BackendClient.Fetch<T>} an object that performs the fetch
   * @template <T> type of Fetch results
   * @protected
   * @abstract
   */
  _fetchOf(query) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a subscription to the topic which is updated with backend changes.
   *
   * @param {!TypedTopic} topic a typed topic which contains runtime information about the subscribed entity type
   * @return {Promise<EntitySubscriptionObject>}
   * @protected
   * @abstract
   */
  _subscribeToTopic(topic) {
    throw new Error('Not implemented in abstract base.');
  }
}

/**
 * @typedef {Fetch} FetchClass
 */

/**
 * Fetches the results of the query from the server using the provided backend.
 *
 * Fetch is a static member of the `BackendClient`.
 *
 * @type FetchClass
 */
BackendClient.Fetch = Fetch;

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
 * Orchestrates the work of the HTTP and Firebase clients and the {@link ActorRequestFactory}.
 */
class FirebaseBackendClient extends BackendClient {

  /**
   * @param {!HttpEndpoint} endpoint the server endpoint to execute queries and commands
   * @param {!FirebaseClient} firebaseClient the client to read the query results from
   * @param {!ActorRequestFactory} actorRequestFactory a factory to instantiate the actor requests with
   * @param {!FirebaseSubscriptionService} subscriptionService a service handling the subscriptions
   */
  constructor(endpoint, firebaseClient, actorRequestFactory, subscriptionService) {
    super(endpoint, actorRequestFactory);
    this._firebase = firebaseClient;
    this._subscriptionService = subscriptionService;
    this._subscriptionService.run();
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
