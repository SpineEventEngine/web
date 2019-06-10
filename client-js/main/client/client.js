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
import {Query} from '../proto/spine/client/query_pb';
import {Topic} from '../proto/spine/client/subscription_pb';

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
 * @typedef {Object} EntitySubscriptionObject
 *
 * An object representing a result of the subscription to entities state changes.
 * The entities that already exist will be initially passed to the `itemAdded` observer.
 *
 * @property {!Observable<T>} itemAdded emits new items matching the subscription topic
 * @property {!Observable<T>} itemChanged emits updated items matching the subscription topic
 * @property {!Observable<T>} itemRemoved emits removed items matching the subscription topic
 * @property {!parameterlessCallback} unsubscribe a method to be called to cancel the subscription,
 *                                   stopping the subscribers from receiving new entities
 *
 * @template <T> a type of the subscription target entities
 */

/**
 * @typedef {Object} SimpleTarget
 *
 * An object representing a set of parameters for building a query or a subscription
 * topic specifying only a type and identifiers of the target entities.
 *
 * Target built from this object point either:
 *  - a single entity of a given type with a given ID;
 *  - several entities of a given type with given IDs;
 *  - all entities of a given type if no IDs specified;
 *
 * @property {!Class<T extends Message>} entity a class of target entities
 * @property {?<I extends Message>[] | <I extends Message> | Number[] | Number | String[] | String} byIds
 *      a list of target entities IDs or an ID of a single target entity
 *
 * @template <T> a class of a query or subscription target entities
 * @template <I> a class of a query or subscription target entities identifiers
 */

/**
 * An abstract client for Spine application backend. This is a single channel for client-server
 * communication in a Spine-based browser application.
 *
 * @abstract
 */
export class Client {

  /**
   * Creates a new {@link QueryFactory} for creating `Query` instances specifying
   * the data to be retrieved from Spine server.
   *
   * @example
   * // Build query specifying entities of a developer-defined `Task` type
   * // by IDs
   * newQuery().select(Task)
   *           .byIds([taskId1, taskId2])
   *           .build()
   *
   * @example
   * // Build query specifying entities of a developer-defined `Task` type
   * // assigned to the specific user
   * newQuery().select(Task)
   *           .where([Filters.eq('assignee', userId)])
   *           .build()
   *
   * To execute the resulting `Query` instance pass it to the {@link Client#execute()}
   * method.
   *
   * @return {QueryFactory} a factory for creating queries to the Spine server
   *
   * @see QueryFactory
   * @see QueryBuilder
   * @see AbstractTargetBuilder
   */
  newQuery() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Executes the given `Query` instance specifying the data to be retrieved from
   * Spine server fulfilling a returned promise with an array of received objects.
   *
   * @param {!Query} query a query instance to be executed
   * @return {Promise<<T extends Message>[]>} a promise to be fulfilled with a list of Protobuf
   *        messages of a given type or with an empty list if no entities matching given query
   *        were found; rejected with a `SpineError` if error occurs;
   *
   * @template <T> a Protobuf type of entities being the target of a query
   */
  execute(query) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a new {@link TopicFactory} for building subscription topics specifying
   * the state changes to be observed from Spine server.
   *
   * @example
   * // Build a subscription topic specifying all entities of a developer-defined `UserTasks` type
   * newQuery().select(Task)
   *           .build()
   *
   * @example
   * // Build a subscription topic specifying entities of a developer-defined `UserTasks`
   * // type where tasks count is greater than 3
   * newTopic().select(UserTasks)
   *           .where(Filters.gt('tasksCount', 3))
   *           .build()
   *
   * To execute the resulting `Topic` instance pass it to the {@link Client#subscribeTo()}
   * method.
   *
   * @return {TopicFactory} a factory for creating subscription topics to the Spine server
   *
   * @see TopicFactory
   * @see TopicBuilder
   * @see AbstractTargetBuilder
   */
  newTopic() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a subscription to the topic which is updated with backend changes.
   * Fulfills a returning promise with an object representing a result of the
   * subscription to entities state changes.
   *
   * @param {!Topic} topic a topic to subscribe
   * @return {Promise<EntitySubscriptionObject<T>>} a promise to be resolved with an object
   *        representing a result of the subscription to entities state changes; rejected with
   *        a `SpineError` if error occurs;
   *
   * @template <T> a Protobuf type of entities being the target of a subscription
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
   * Fetches entities of the given class type from the Spine backend. Fetches
   * single or several entities if `byIds` specified, and all entities of the given type
   * when no IDs specified.
   *
   * Fulfills a returned promise with an array of received objects when fetches several
   * or all entities of type and with a single entity when fetches by single ID.
   *
   * This method shortens a two-step query execution with {@link Client#newQuery()}
   * and {@link Client#execute()} methods. Should be used for common queries when there's no
   * need in query with filters or masks applied.
   *
   * @example
   * // Fetch all entities of a developer-defined `Task` type. Returning promise
   * // resolves with a list of entities or with an empty list if no records of specified
   * // type were found.
   * fetch({entity: Task}).then(tasks => { ... })
   *
   * @example
   * // Fetch a single entity of a developer-defined `Task` type by ID. Returning promise
   * // resolves with a received entity or with `null` if no entity with specified
   * // ID was found.
   * fetch({entity: Task, byIds: taskId}).then(task => { ... })
   *
   * @example
   * // Fetch several entities of a developer-defined `Task` type by IDs. Returning promise
   * // resolves with a list of entities or with an empty list if no records with specified
   * // IDs were found.
   * fetch({entity: Task, byIds: [taskId1, taskId2]}).then(tasks => { ... })
   *
   * @param {SimpleTarget<T>} object representing a set of parameters for building a query by target
   *      entities type and IDs
   * @return {Promise<T[] | T | null>} a promise to be fulfilled with a list of Protobuf messages
   *        of a given type or with a single entity if fetched by ID; resolves with an empty list
   *        if no entities matching given class or IDs were found; resolves with `null` if no
   *        entity with a specified ID was found; rejected with a `SpineError` if error occurs;
   *
   * @template <T> a Protobuf type of entities being the fetch target
   */
  fetch({entity: cls, byIds: ids}) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a subscription to changes of entities of given type. Subscribes to changes of
   * a single or several entities if `byIds` specified, and all entities of the given type when
   * no IDs specified. Fulfills a returning promise with an object representing a result of the
   * subscription to entities state changes.
   *
   * The entities that already exist will be initially passed to the `itemAdded` observer.
   *
   * This method shortens a two-step subscription to topic with {@link Client#newTopic()}
   * and {@link Client#subscribeTo()} methods. Should be used for common subscriptions when there's
   * no need to create subscription with filters or masks applied.
   *
   * @example
   * // Subscribe to changes of all entities of a developer-defined `UserTasks` type. Returning
   * // promise resolves with an object representing a result of the subscription.
   * subscribe({entity: UserTasks}).then(subscriptionObject => { ... })
   *
   * @example
   * // Subscribe to changes of a single entity of a developer-defined `UserTasks` type by ID.
   * subscribe({entity: Task, byIds: taskId}).then(subscriptionObject => { ... })
   *
   * @example
   * // Subscribe to changes of several entities of a developer-defined `UserTasks` type by IDs.
   * subscribe({entity: Task, byIds: [taskId1, taskId2]}).then(subscriptionObject => { ... })
   *
   * @param {SimpleTarget<T>} object representing a set of parameters for building a subscription
   *    topic by target entities type and IDs
   * @return {Promise<EntitySubscriptionObject<T>>} a promise of means to observe the changes
   *                                             and unsubscribe from the updated
   *
   * @template <T> a Protobuf type of entities being the subscription target
   */
  subscribe({entity: cls, byIds: ids}) {
    throw new Error('Not implemented in abstract base.');
  }
}
