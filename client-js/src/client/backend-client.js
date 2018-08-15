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

import {Observable} from './observable';
import {TypedMessage, TypeUrl} from './typed-message';
import {HttpEndpoint, QUERY_STRATEGY} from './http-endpoint';
import {HttpClient} from './http-client';
import {FirebaseClient} from './firebase-client';
import {ActorRequestFactory} from './actor-request-factory';


/**
 * An abstract Fetch that can fetch the data of a provided query in one of two ways
 * (one-by-one or all-at-once) using the provided backend.
 *
 * Fetch is a static member of the `BackendClient`.
 *
 * @template <T>
 */
class Fetch {

  /**
   * @param {!Query} query a query to be performed by Spine server
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
   * @returns {Observable<Object, EndpointError>} an observable retrieving values one at a time.
   * @abstract
   */
  oneByOne() {
    throw 'Not implemented in abstract base.';
  }

  /**
   * Fetches all query results at once fulfilling a promise with an array of entities.
   *
   * @example
   * // To query all entities of developer-defined Task type at once:
   * fetchAll({ofType: taskType}).atOnce().then(tasks => { ... })
   *
   * @returns {Promise<Object[]>} a promise resolving an array of entities matching query,
   *                              that be rejected with an `EndpointError`
   * @abstract
   */
  atOnce() {
    throw 'Not implemented in abstract base.';
  }
}

/**
 * An abstract client for Spine application backend. This is a single channel for client-server
 * communication in a Spine-based browser application.
 *
 * Backend client defines operations that client is able to perform (`.fetchAll(...)`,
 * `.sendCommand(...)`, etc.), also providing factory methods for creating Backend Client
 * instances (`.usingFirebase(...)`).
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
   * @param {!TypeUrl<T>} ofType a type of the entities to be queried
   * @returns {BackendClient.Fetch<T>} a fetch object allowing to specify additional remote
   *                                call parameters and executed the query.
   *
   * @template <T>
   */
  fetchAll({ofType: typeUrl}) {
    const query = this._requestFactory.query().select(typeUrl).build();
    return this._fetchOf(query);
  }

  /**
   * Fetches a single entity of the given type.
   *
   * @param {!TypeUrl<T>} type a type URL of the target entity
   * @param {!TypedMessage} id an ID of the target entity
   * @param {!consumerCallback<Object>} dataCallback
   *        a callback receiving a single data item as a JS object
   * @param {?consumerCallback<EndpointError>} errorCallback
   *        a callback receiving an error
   *
   * @template <T>
   */
  fetchById(type, id, dataCallback, errorCallback) {
    const query = this._requestFactory.query().select(type).byId(id).build();

    const observer = {next: dataCallback};
    if (errorCallback) {
      observer.error = errorCallback;
    }
    // noinspection JSCheckFunctionSignatures
    this._fetchOf(query).oneByOne().subscribe(observer);
  }

  /**
   * Sends the provided command to the server.
   *
   * @param {!TypedMessage} commandMessage a typed command message
   * @param {!voidCallback} successCallback
   *        a no-argument callback invoked if the command is acknowledged
   * @param {?consumerCallback<spine.base.Error>} errorCallback
   *        a callback receiving the errors executed if an error occured when processing command
   * @param {?consumerCallback<Rejection>} rejectionCallback
   *        a callback executed if the command was rejected by Spine server
   */
  sendCommand(commandMessage, successCallback, errorCallback, rejectionCallback) {
    const command = this._requestFactory.command(commandMessage);
    this._endpoint.command(command)
      .then(ack => {
        const status = ack.status;
        if (status.hasOwnProperty('ok')) {
          successCallback();
        } else if (status.hasOwnProperty('error')) {
          errorCallback(status.error);
        } else if (status.hasOwnProperty('rejection')) {
          rejectionCallback(status.rejection);
        }
      }, errorCallback);
  }

  /**
   * A static factory method that creates a new `BackendClient` instance using Firebase as
   * underlying implementation.
   *
   * @param {!string} atEndpoint a Spine web backend endpoint URL
   * @param {!firebase.app.App} withFirebaseStorage
   *        a Firebase Application that will be used to retrieve data from
   * @param {!string} forActor an id of the user interacting with Spine
   * @returns {BackendClient} a new backend client instance which will send the requests on behalf
   *                          of the provided actor to the provided endpoint, retrieving the data
   *                          from the provided Firebase storage
   */
  static usingFirebase({atEndpoint: endpointUrl, withFirebaseStorage: firebaseApp, forActor: actor}) {
    const httpClient = new HttpClient(endpointUrl);
    const endpoint = new HttpEndpoint(httpClient);
    const firebaseClient = new FirebaseClient(firebaseApp);
    const requestFactory = new ActorRequestFactory(actor);

    return new FirebaseBackendClient(endpoint, firebaseClient, requestFactory)
  }

  /**
   * Creates a new Fetch object specifying the target of fetch and its parameters.
   *
   * @param {!Query} query a query processed by Spine
   * @returns {BackendClient.Fetch<T>} an object that performs the fetch
   * @template <T> type of Fetch results
   * @protected
   * @abstract
   */
  _fetchOf(query) {
    throw 'Not implemented in abstract base.';
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
   * @param {!Query} query a query to be performed by Spine server
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

  _fetchManyOneByOne() {
    return new Observable(observer => {

      let receivedCount = 0;
      let promisedCount = null;
      let dbSubscription = null;

      this._backend._endpoint.query(this._query, QUERY_STRATEGY.oneByOne)
        .then(({path, count}) => {
          promisedCount = count;
          return path;
        })
        .then(path => {
          dbSubscription = this._backend._firebase.onChildAdded(path, value => {
            observer.next(value);
            receivedCount++;
            if (receivedCount === promisedCount) {
              observer.complete();
              dbSubscription.unsubscribe();
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

  _fetchManyAtOnce() {
    return new Promise((resolve, reject) => {
      this._backend._endpoint.query(this._query, QUERY_STRATEGY.allAtOnce)
        .then(({path}) => this._backend._firebase.getValue(path, resolve))
        .catch(error => reject(error));
    });
  }
}

/**
 *
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
   */
  constructor(endpoint, firebaseClient, actorRequestFactory) {
    super(endpoint, actorRequestFactory);
    this._firebase = firebaseClient;
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
}

/**
 * @inheritDoc
 * @type FetchClass
 */
FirebaseBackendClient.Fetch = FirebaseFetch;

