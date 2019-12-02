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
 * @typedef {Object} EventSubscriptionObject
 *
 * @property <!Observable<spine.core.Event>> eventEmitted
 * @property {!parameterlessCallback} unsubscribe
 */

/**
 * @typedef AckCallback
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
   * @param {!Class<? extends Message>} entityType a Protobuf type of the query target entities
   * @return {QueryRequest}
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
   * @param {!Class<? extends Message>} entityType a Protobuf type of the target entities
   * @return {SubscriptionRequest}
   */
  subscribeTo(entityType) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!Class<? extends Message>} eventType a Protobuf type of the target events
   * @return {EventSubscriptionRequest}
   */
  subscribeToEvent(eventType) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!spine.client.Topic} topic
   *
   * @return {Promise<EntitySubscriptionObject<T extends Message>>}
   *
   * @template <T> a Protobuf type of entities being the target of a subscription
   */
  subscribe(topic) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!spine.client.Topic} topic
   *
   * @return {Promise<EventSubscriptionObject>}
   */
  subscribeToEvents(topic) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!Message} commandMessage a Protobuf type of the query target entities
   * @return {CommandRequest}
   */
  command(commandMessage) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!spine.core.Command} command a Command sent to Spine server
   * @param {!AckCallback} ackCallback
   */
  post(command, ackCallback) {
    throw new Error('Not implemented in abstract base.');
  }
}
