/*
 * Copyright 2021, TeamDev. All rights reserved.
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
 * The callback that accepts a single `Event` as a parameter.
 *
 * @callback eventConsumer
 *
 * @param {spine.core.Event} the event that is accepted by the callback
 */

/**
 * @typedef {Object} EntitySubscriptionObject
 *
 * An object representing the result of a subscription to entity state changes.
 *
 * @property {!Observable<T>} itemAdded emits new items matching the subscription topic
 * @property {!Observable<T>} itemChanged emits updated items matching the subscription topic
 * @property {!Observable<T>} itemRemoved emits removed items matching the subscription topic
 * @property {!parameterlessCallback} unsubscribe a method to be called to cancel the subscription,
 *                                                stopping the subscribers from receiving new
 *                                                entities
 *
 * @template <T> a type of the subscription target entities
 */

/**
 * @typedef {Object} EventSubscriptionObject
 *
 * An object which represents the result of a subscription to events of a certain type.
 *
 * @property <!Observable<spine.core.Event>> eventEmitted emits new items when the new events
 *                                                        matching the subscription topic occur in
 *                                                        the system
 * @property {!parameterlessCallback} unsubscribe a method to be called to cancel the subscription,
 *                                                stopping the subscribers from receiving new
 *                                                entities
 */

/**
 * @typedef AckCallback
 *
 * Represents a command acknowledgement callback.
 *
 * @property {!parameterlessCallback} onOk
 *           the callback to run when the command is handled properly
 * @property {!consumerCallback<Error>} onError
 *           the callback to run when the command cannot be handled due to a technical error
 * @property {!consumerCallback<Message>} onImmediateRejection
 *           the callback to run when the command is denied execution due to a business rejection
 */

/**
 * An abstract client for Spine application backend. This is a single channel for client-server
 * communication in a Spine-based browser application.
 *
 * @abstract
 */
export class Client {

  /**
   * Creates a query request that allows to configure and post a new query.
   *
   * @param {!Class<Message>} entityType a Protobuf type of the query target entities
   * @return {QueryRequest} the builder to construct and post a new query
   */
  select(entityType) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Executes the given `Query` instance specifying the data to be retrieved from
   * Spine server fulfilling a returned promise with an array of received objects.
   *
   * @param {!spine.client.Query} query a query instance to be executed
   * @return {Promise<Message[]>} a promise to be fulfilled with a list of Protobuf
   *        messages of a given type or with an empty list if no entities matching given query
   *        were found; rejected with a `SpineError` if error occurs
   *
   * @template <T> a Protobuf type of entities being the target of a query
   */
  read(query) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Executes the given `Query` instance specifying the data to be retrieved from
   * Spine server fulfilling a returned promise with an array of received objects.
   *
   * @param {!spine.client.Query} query a query instance to be executed
   * @return {Promise<Message[]>} a promise to be fulfilled with a list of Protobuf
   *        messages of a given type or with an empty list if no entities matching given query
   *        were found; rejected with a `SpineError` if error occurs;
   *
   * @template <T> a Protobuf type of entities being the target of a query
   *
   * @deprecated Please use {@link Client#read()} instead
   */
  execute(query) {
    return this.read(query);
  }

  /**
   * Creates a new {@link QueryFactory} for creating `Query` instances specifying
   * the data to be retrieved from Spine server.
   *
   * @example
   * // Build a query for `Task` domain entity, specifying particular IDs.
   * newQuery().select(Task)
   *           .byIds([taskId1, taskId2])
   *           .build()
   *
   * @example
   * // Build a query for `Task` domain entity, selecting the instances which assigned to the
   * // particular user.
   * newQuery().select(Task)
   *           .where([Filters.eq('assignee', userId)])
   *           .build()
   *
   * To execute the resulting `Query` instance pass it to the {@link Client#execute()}.
   *
   * Alternatively, the `QueryRequest` API can be used. See {@link Client#select()}.
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
   * Creates a subscription request that allows to configure and post a new entity subscription.
   *
   * @param {!Class<Message>} entityType a Protobuf type of the target entities
   * @return {SubscriptionRequest} the builder for the new entity subscription
   */
  subscribeTo(entityType) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Subscribes to the given `Topic` instance.
   *
   * The topic should have an entity type as target. Use {@link #subscribeToEvents} to subscribe to
   * the topic that targets events.
   *
   * @param {!spine.client.Topic} topic a topic to subscribe to
   * @return {Promise<EntitySubscriptionObject<Message>>}
   *         the subscription object which exposes entity changes via its callbacks
   *
   * @template <T> a Protobuf type of entities being the target of a subscription
   */
  subscribe(topic) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates an event subscription request that allows to configure and post a new event
   * subscription.
   *
   * @param {!Class<Message>} eventType a Protobuf type of the target events
   * @return {EventSubscriptionRequest} the builder for the new event subscription
   */
  subscribeToEvent(eventType) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Subscribes to the given `Topic` instance.
   *
   * The given topic should target an event type. To perform an entity subscription, use
   * {@link #subscribe}.
   *
   * @param {!spine.client.Topic} topic a topic to subscribe to
   *
   * @return {Promise<EventSubscriptionObject>}
   */
  subscribeToEvents(topic) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a new {@link TopicFactory} for building subscription topics specifying
   * the state changes to be observed from Spine server.
   *
   * @example
   * // Build a subscription topic for `UserTasks` domain entity.
   * newTopic().select(Task)
   *           .build()
   *
   * @example
   * // Build a subscription topic for `UserTasks` domain entity, selecting the instances
   * // with over 3 tasks.
   * newTopic().select(UserTasks)
   *           .where(Filters.gt('tasksCount', 3))
   *           .build()
   *
   * To turn the resulting `Topic` instance into a subscription pass it
   * to the {@link Client#subscribe()}.
   *
   * Alternatively, the `SubscriptionRequest` API can be used. See {@link Client#subscribeTo()},
   * {@link Client#subscribeToEvents()}.
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
   * Creates a new command request which allows to post a command to the Spine server and
   * configures the command handling callbacks.
   *
   * @param {!Message} commandMessage a command to post to the server
   * @return {CommandRequest} a new command request
   */
  command(commandMessage) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Posts a given command to the Spine server.
   *
   * @param {!spine.core.Command} command a Command sent to Spine server
   * @param {!AckCallback} onAck a command acknowledgement callback
   */
  post(command, onAck) {
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
   * If the command sending fails, the respective error is passed to the `errorCallback`. This error
   * is always the type of `CommandHandlingError`. Its cause can be retrieved by `getCause()` method
   * and can be represented with the following types of errors:
   *
   *  - `ConnectionError`  – if the connection error occurs;
   *  - `ClientError`      – if the server responds with `4xx` HTTP status code;
   *  - `ServerError`      – if the server responds with `5xx` HTTP status code;
   *  - `spine.base.Error` – if the command message can't be processed by the server;
   *  - `SpineError`       – if parsing of the response fails;
   *
   * If the command sending fails due to a command validation error, an error passed to the
   * `errorCallback` is the type of `CommandValidationError` (inherited from
   * `CommandHandlingError`). The validation error can be retrieved by `validationError()` method.
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
   *        a callback executed if the command is denied processing due to a business rejection
   * @see CommandHandlingError
   * @see CommandValidationError
   *
   * @deprecated Please use {@link Client#command()}
   */
  sendCommand(commandMessage, acknowledgedCallback, errorCallback, rejectionCallback) {
    this.command(commandMessage)
        .onOk(acknowledgedCallback)
        .onError(errorCallback)
        .onImmediateRejection(rejectionCallback)
        .post();
  }
}
