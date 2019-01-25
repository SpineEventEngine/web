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

/**
 * The callback that doesn't accept any parameters.
 * @callback parameterlessCallback
 */

/**
 * The callback that accepts single parameter.
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
 * @property {parameterlessCallback} unsubscribe a method to be called to cancel the subscription, stopping
 *                                      the subscribers from receiving new entities
 *
 * @template <T>
 */

/**
 * An abstract Fetch that can fetch the data of a provided query in one of two ways
 * (one-by-one or all-at-once) using the provided backend.
 *
 * Fetch is a static member of the `BackendClient`.
 *
 * @template <T>
 * @abstract
 */
export class Fetch {

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
 * An abstract client for Spine application backend. This is a single channel for client-server
 * communication in a Spine-based browser application.
 *
 * Protobuf types that will be used with the client should be registered via `registerTypes(...)`.
 *
 * @abstract
 */
export class BackendClient {

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
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Fetches a single entity of the given type.
   *
   * @param {!Type<T>} type a type URL of the target entity
   * @param {!Message} id an ID of the target entity
   * @param {!consumerCallback<Message>>} dataCallback
   *        a callback receiving a single data item as a Protobuf message of a given type; receives `null` if an
   *        entity with a given ID is missing
   * @param {?consumerCallback<SpineError>} errorCallback
   *        a callback receiving an error
   *
   * @template <T>
   */
  fetchById(type, id, dataCallback, errorCallback) {
    throw new Error('Not implemented in abstract base.');
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
   * @param {!parameterlessCallback} acknowledgedCallback
   *        a no-argument callback invoked if the command is acknowledged
   * @param {?consumerCallback<CommandHandlingError>} errorCallback
   *        a callback receiving the errors executed if an error occurred when sending command
   * @param {?consumerCallback<Rejection>} rejectionCallback
   *        a callback executed if the command was rejected by Spine server
   * @see CommandHandlingError
   * @see CommandValidationError
   */
  sendCommand(commandMessage, acknowledgedCallback, errorCallback, rejectionCallback) {
    throw new Error('Not implemented in abstract base.');
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
