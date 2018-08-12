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

import {Observable} from "./observable";
import {TypedMessage, TypeUrl} from "./typed-message";
import {QUERY_STRATEGY} from "./endpoint";

/**
 * @callback successfulCommandCallback a callback without any arguments,
 */

/**
 * @callback commandErrorCallback a callback receiving Spine Error message as parameter
 * @param {spine.base.Error} error a Spine Error message
 */

/**
 * @callback commandRejectionCallback a callback receiving Spine Rejection message as parameter
 * @param {spine.core.Rejection} error a Spine Rejection message
 */

/**
 * Fetches the results of the query from the server using the provided backend.
 *
 * Fetch is a static member of the `BackendClient`.
 */
class Fetch {

  /**
   * @param {!TypedMessage<Query>} query a query to be performed by Spine
   * @param {!BackendClient} backend the backend which is used to fetch the query results
   */
  constructor({of: query, using: backend}) {
    this._query = query;
    this._backend = backend;
  }

  /**
   * Fetches items one-by-one using an Observable.
   * Suitable for big collections.
   *
   * @returns {Observable<Object>} an Observable retrieving values one at a time.
   * @example
   * fetchAll({ofType: taskType}).oneByOne().subscribe({
   *   next(value) { ... },
   *   error(error) { ... },
   *   complete() { ... }
   * })
   */
  oneByOne() {
    return this._fetchManyOneByOne();
  }

  /**
   * Fetches all query results at once resolving a promise with an array of entities.
   *
   * @returns {Promise<Object[]>} a Promise resolving an array of items matching query.
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
 * The client of the application backend.
 *
 * Orchestrates the work of the HTTP and Firebase clients and
 * the {@link ActorRequestFactory}.
 */
export class BackendClient {

  /**
   * @param {!Endpoint} endpoint the server endpoint to execute queries and commands
   * @param {!FirebaseClient} firebaseClient the client to read the query results from
   * @param {!ActorRequestFactory} actorRequestFactory a factory to instantiate the actor requests with
   */
  constructor(endpoint, firebaseClient, actorRequestFactory) {
    this._endpoint = endpoint;
    this._firebase = firebaseClient;
    this._actorRequestFactory = actorRequestFactory;
  }

  /**
   * Defines a fetch query of all entities matching the filters provided as arguments.
   * This fetch is executed later upon calling 
   *
   * @param {!TypeUrl} ofType a type of the entities to be queried
   * @returns {BackendClient.Fetch} a fetch object allowing to specify additional remote
   *                                call parameters and executed the query.
   * @example
   * // Fetch items one-by-one using an Observable.
   * // Suitable for big collections.
   * fetchAll({ofType: taskType}).oneByOne().subscribe({
   *   next(value) { ... },
   *   error(error) { ... },
   *   complete() { ... }
   * })
   * @example
   * // Fetch all items at once using a Promise.
   * fetchAll({ofType: taskType}).atOnce().then(data => { ... })
   */
  fetchAll({ofType: typeUrl}) {
    const query = this._actorRequestFactory.newQueryForAll(typeUrl);
    // noinspection JSValidateTypes A static member class type is not resolved properly.
    return new BackendClient.Fetch({of: query, using: this});
  }

  /**
   * Fetches a single entity of the given type.
   *
   * @param {!TypeUrl<T>} type a type URL of the target entity
   * @param {!TypedMessage} id an ID of the target entity
   * @param {!nextCallback<T>} dataCallback a callback receiving a single data item as a JS object
   * @param {?errorCallback} errorCallback a callback receiving an error
   * 
   * @template <T>
   */
  fetchById(type, id, dataCallback, errorCallback) {
    const query = this._actorRequestFactory.queryById(type.value, id);
    const fetch = new Fetch({of: query, using: this});

    // noinspection JSCheckFunctionSignatures
    return fetch.oneByOne().subscribe({
      next: dataCallback,
      error: errorCallback || undefined
    });
  }

  /**
   * Sends the provided command to the server.
   *
   * @param {!TypedMessage} commandMessage a typed command message
   * @param {!successfulCommandCallback} successCallback a no-argument callback invoked if
   *                                                     the command is acknowledged
   * @param {?commandErrorCallback} errorCallback a callback which receives the errors executed if 
   *                                              an error occcured when processing command  
   * @param {?commandRejectionCallback} rejectionCallback a callback executed if the command was
   *                                                      rejected by Spine
   */
  sendCommand(commandMessage, successCallback, errorCallback, rejectionCallback) {
    const command = this._actorRequestFactory.command(commandMessage);
    this._endpoint.command(command)
      .then(ack => {
        const status = ack.status;
        if (status.hasOwnProperty("ok")) {
          successCallback();
        } else if (status.hasOwnProperty("error")) {
          errorCallback(status.error);
        } else if (status.hasOwnProperty("rejection")) {
          rejectionCallback(status.rejection);
        }
      }, errorCallback);
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

