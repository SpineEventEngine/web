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
 * The client of the application backend.
 *
 * Orchestrates the work of the HTTP and Firebase clients and
 * the {@link ActorRequestFactory}.
 */
export class BackendClient {

  /**
   * Creates a new `BackendClient`.
   *
   * @param endpoint {Endpoint} the server endpoint to execute queries and commands 
   * @param firebaseClient {FirebaseClient} the client to read the query results from
   * @param actorRequestFactory {ActorRequestFactory} a factory to instantiate the actor requests with
   */
  constructor(endpoint, firebaseClient, actorRequestFactory) {
    this._endpoint = endpoint;
    this._firebase = firebaseClient;
    this._actorRequestFactory = actorRequestFactory;
  }

  /**
   * Defines a fetch query of all objects of specified type.
   *
   * @param ofType {TypeUrl} a type of the entities to be queried
   * @returns {{oneByOne, atOnce}} an object allowing two fetch strategies: one-by-one or all-at-once.
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
    return {
      /**
       * @returns {Observable} an Observable retrieving values one at a time.
       */
      oneByOne: () => this._fetchManyOneByOne(query),
      /**
       * @returns {Promise} a Promise resolving an array of items matching query.
       */
      atOnce: () => this._fetchManyAtOnce(query)
    };
  }

  /**
   * Fetches a single entity of the given type.
   *
   * @param type          the target {@link TypeUrl}
   * @param id            the target entity ID, as a {@link TypedMessage}
   * @param dataCallback  the callback which receives the single data item, in
   *                      a form of a JS object
   * @param errorCallback the callback which receives the errors
   */
  fetchById(type, id, dataCallback, errorCallback = null) {
    const query = this._actorRequestFactory.queryById(type.value, id);
    this._fetch(query, dataCallback, errorCallback);
  }

  /**
   * Sends the given command to the server.
   *
   * @param commandMessage    the {@link TypedMessage} representing the command message
   * @param successListener   the no-argument callback invoked if the command
   *                          is acknowledged
   * @param errorCallback     the callback which receives the errors
   * @param rejectionCallback the callback which receives the command rejections
   */
  sendCommand(commandMessage, successListener, errorCallback, rejectionCallback) {
    const command = this._actorRequestFactory.command(commandMessage);
    this._endpoint.command(command)
      .then(ack => {
        const status = ack.status;
        if (status.hasOwnProperty("ok")) {
          successListener();
        } else if (status.hasOwnProperty("error")) {
          errorCallback(status.error);
        } else if (status.hasOwnProperty("rejection")) {
          rejectionCallback(status.rejection);
        }
      }, errorCallback);
  }

  _fetch(query, dataCallback, errorCallback = null) {
    const onError = errorCallback || (() => {});

    this._endpoint.query(query, QUERY_STRATEGY.oneByOne)
      .then(({path}) => this._firebase.onChildAdded(path, dataCallback), onError);
  }

  _fetchManyOneByOne(query) {
    return new Observable(observer => {

      let receivedCount = 0;
      let promisedCount = null;
      let dbSubscription = null;

      this._endpoint.query(query, QUERY_STRATEGY.oneByOne)
        .then(({path, count}) => {
          promisedCount = count;
          return path;
        })
        .then(path => {
          dbSubscription = this._firebase.onChildAdded(path, value => {
            observer.next(value);
            receivedCount++;
            if (receivedCount === promisedCount) {
              observer.complete();
              dbSubscription.unsubscribe();
            }
          });
        })
        .catch(observer.error);

      // Returning tear down.
      return () => {
        if (dbSubscription) {
          dbSubscription.unsubscribe();
        }
      }
    });
  }

  _fetchManyAtOnce(query) {
    return new Promise((resolve, reject) => {
      this._endpoint.query(query, QUERY_STRATEGY.allAtOnce)
        .then(({path}) => this._firebase.getValue(path, resolve))
        .catch(error => reject(error));
    });
  }
}
