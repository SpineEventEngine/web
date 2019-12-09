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
 * @typedef {Object} EntitySubscriptionObject
 *
 * An object representing a result of the subscription to entity state changes.
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
 * An object which represents a result of the subscription to events of a certain type.
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
 * @property {!consumerCallback<Error>} onError
 * @property {!consumerCallback<Message>} onRejection
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
   * @param {!Class<? extends Message>} entityType a Protobuf type of the query target entities
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
   * @return {Promise<<T extends Message>[]>} a promise to be fulfilled with a list of Protobuf
   *        messages of a given type or with an empty list if no entities matching given query
   *        were found; rejected with a `SpineError` if error occurs
   *
   * @template <T> a Protobuf type of entities being the target of a query
   */
  read(query) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a subscription request that allows to configure and post a new entity subscription.
   *
   * @param {!Class<? extends Message>} entityType a Protobuf type of the target entities
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
   * @return {Promise<EntitySubscriptionObject<T extends Message>>} the subscription object which
   *                                                                exposes entity changes via its
   *                                                                callbacks
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
   * @param {!Class<? extends Message>} eventType a Protobuf type of the target events
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
   * Posts the given command to the Spine server.
   *
   * @param {!spine.core.Command} command a Command sent to Spine server
   * @param {!AckCallback} onAck a command acknowledgement callback
   */
  post(command, onAck) {
    throw new Error('Not implemented in abstract base.');
  }
}
