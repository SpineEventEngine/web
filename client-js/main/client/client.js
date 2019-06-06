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

import {Message} from 'google-protobuf';
import {Observable} from 'rxjs';

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
 * @template <T> the type of value passed to the callback
 */

/**
 * @typedef {Object} EntitySubscriptionObject an object representing a result of the subscription
 *                                            to entities state changes
 * @property {Observable<T>} itemAdded
 * @property {Observable<T>} itemChanged
 * @property {Observable<T>} itemRemoved
 * @property {parameterlessCallback} unsubscribe a method to be called to cancel the subscription,
 *                                   stopping the subscribers from receiving new entities
 *
 * @template <T> a type of the subscription target entities
 */

/**
 * @typedef {Object} TargetCriteria
 *
 * @property {!Class<T extends Message>} entity
 * @property {?<I extends Message>[] | Number[] | String[]} byIds
 * @property {?<I extends Message>[] | Number[] | String[]} byId
 *
 * @template <T> a type of the subscription target entities
 * @template <I>
 */

/**
 * An abstract client for Spine application backend. This is a single channel for client-server
 * communication in a Spine-based browser application.
 *
 * @abstract
 */
export class Client {

  /**
   *
   */
  newQuery() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   *
   * @param query
   */
  execute(query) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   *
   */
  newTopic() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a subscription to the topic which is updated with backend changes.
   *
   * @param {!spine.client.Topic} topic a topic of a subscription
   * @return {Promise<EntitySubscriptionObject>}
   * @abstract
   */
  subscribeTo(topic) {
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
   * @param {!Message} commandMessage a Protobuf message representing the command
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
   * Fetches entities of the given class type fulfilling a returned promise
   * with an array of received objects.
   *
   * @example
   * // Fetch all entities of a developer-defined Task type at once using a Promise.
   * fetch({entity: Task}).then(tasks => { ... })
   *
   * @param {TargetCriteria}
   * @return {Promise<T[] | T | null>} a promise to be fulfilled with a list of Protobuf messages
   *        of a given type or with an empty list if no entities matching given class were found;
   *        rejected with a `SpineError` if error occurs;
   *
   * @template <T>
   */
  fetch({entity: cls, byIds: ids, byId: id}) {
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
   * @param {TargetCriteria}
   * @return {Promise<EntitySubscriptionObject>} a promise of means to observe the changes
   *                                             and unsubscribe from the updated
   */
  subscribe({entity: cls, byIds: ids, byId: id}) {
    throw new Error('Not implemented in abstract base.');
  }
}
