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
import ObjectToProto from "./object-to-proto";
import {CommandHandlingError, CommandValidationError, SpineError} from "./errors";
import {Status} from '../proto/spine/core/response_pb';
import {Client} from "./client";
import {TypedMessage} from "./typed-message";
import {QueryRequest} from "./query-request";
import {EventSubscriptionRequest, SubscriptionRequest} from "./subscribing-request";
import {CommandRequest} from "./command-request";

/**
 * A {@link Client} that delegates requests to case-specific client implementations.
 *
 * @see QueryingClient
 * @see SubscribingClient
 * @see CommandingClient
 */
export class CompositeClient extends Client {

  constructor(querying, subscribing, commanding) {
    super();

    /**
     * @type QueryingClient
     *
     * @private
     */
    this._querying = querying;

    /**
     * @type SubscribingClient
     *
     * @private
     */
    this._subscribing = subscribing;

    /**
     * @type CommandingClient
     *
     * @private
     */
    this._commanding = commanding;
  }

  /**
   * @override
   */
  select(entityType) {
    return this._querying.select(entityType, this);
  }

  /**
   * @override
   */
  read(query) {
    return this._querying.read(query);
  }

  /**
   * @override
   */
  newQuery() {
    return this._querying.newQuery();
  }

  /**
   * @override
   */
  subscribeTo(entityType) {
    return this._subscribing.subscribeTo(entityType, this);
  }

  /**
   * @override
   */
  subscribeToEvent(eventType) {
    return this._subscribing.subscribeToEvent(eventType, this);
  }

  /**
   * @override
   */
  subscribe(topic) {
    return this._subscribing.subscribe(topic)
  }

  /**
   * @override
   */
  subscribeToEvents(topic) {
    return this._subscribing.subscribeToEvents(topic);
  }

  /**
   * @override
   */
  newTopic() {
    return this._subscribing.newTopic();
  }

  /**
   * @override
   */
  command(commandMessage) {
    return this._commanding.command(commandMessage, this);
  }

  /**
   * @override
   */
  post(command, onAck) {
    this._commanding.post(command, onAck);
  }
}

/**
 * A client which performs entity state queries.
 *
 * @abstract
 */
export class QueryingClient {

  /**
   * @param {!ActorRequestFactory} actorRequestFactory
   *        a request factory to build requests to Spine server
   */
  constructor(actorRequestFactory) {
    this._requestFactory = actorRequestFactory;
  }

  /**
   * Creates a new query request.
   *
   * @param {!Class<? extends Message>} entityType the target entity type
   * @param {!Client} client the client which initiated the request
   * @return {QueryRequest} a new query request
   */
  select(entityType, client) {
    return new QueryRequest(entityType, client, this._requestFactory);
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
   * Creates a new query factory instance which can be further used for the `Query` creation.
   *
   * @return {QueryFactory}
   */
  newQuery() {
    return this._requestFactory.query();
  }
}

/**
 * A client which manages entity state and event subscriptions.
 *
 * @abstract
 */
export class SubscribingClient {

  /**
   * @param {!ActorRequestFactory} actorRequestFactory
   *        a request factory to build requests to Spine server
   */
  constructor(actorRequestFactory) {
    this._requestFactory = actorRequestFactory;
  }

  /**
   * Creates a new subscription request.
   *
   * @param {!Class<? extends Message>} type the target entity type
   * @param {!Client} client the client that initiated the request
   * @return {SubscriptionRequest} a new subscription request
   */
  subscribeTo(type, client) {
    return new SubscriptionRequest(type, client, this._requestFactory);
  }

  /**
   * Subscribes to a given topic which targets an entity type.
   *
   * @param {!spine.client.Topic} topic a topic to subscribe to
   * @return {Promise<EntitySubscriptionObject<T extends Message>>} a subscription object
   *
   * @template <T> a Protobuf type of entities being the target of a subscription
   */
  subscribe(topic) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a new event subscription request.
   *
   * @param {!Class<? extends Message>} type the target event type
   * @param {!Client} client the client that initiated the request
   * @return {EventSubscriptionRequest} a new event subscription request
   */
  subscribeToEvent(type, client) {
    return new EventSubscriptionRequest(type, client, this._requestFactory);
  }

  /**
   * Subscribes to the given topic which targets an event type.
   *
   * @param {!spine.client.Topic} topic a topic to subscribe to
   * @return {Promise<EventSubscriptionObject>} a subscription object
   */
  subscribeToEvents(topic) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Returns a new topic factory instance which can be further used for the `Topic` creation.
   *
   * @return {TopicFactory}
   */
  newTopic() {
    return this._requestFactory.topic();
  }
}

const SUBSCRIPTIONS_NOT_SUPPORTED = 'Subscriptions are not supported.';

/**
 * A {@link SubscribingClient} which does not create subscriptions.
 */
export class NoOpSubscribingClient extends SubscribingClient {

  constructor(actorRequestFactory) {
    super(actorRequestFactory)
  }

  /**
   * Always throws an error.
   *
   * @override
   */
  subscribe(topic) {
    throw new Error(SUBSCRIPTIONS_NOT_SUPPORTED);
  }

  /**
   * Always throws an error.
   *
   * @override
   */
  subscribeToEvents(topic) {
    throw new Error(SUBSCRIPTIONS_NOT_SUPPORTED);
  }
}

const _statusType = Status.typeUrl();

/**
 * A client which posts commands.
 *
 * This class has a default implementation. Override it to change the behaviour.
 */
export class CommandingClient {

  constructor(endpoint, requestFactory) {
    this._requestFactory = requestFactory;
    this._endpoint = endpoint;
  }

  /**
   * Creates a new command request.
   *
   * @param {!Message} commandMessage the command to send to the server
   * @param {!Client} client the client which initiated the request
   * @return {CommandRequest} a new command request
   */
  command(commandMessage, client) {
    return new CommandRequest(commandMessage, client, this._requestFactory);
  }

  /**
   * Posts a given command to the Spine server.
   *
   * @param {!spine.core.Command} command a Command sent to Spine server
   * @param {!AckCallback} onAck a command acknowledgement callback
   */
  post(command, onAck) {
    const cmd = TypedMessage.of(command);
    this._endpoint.command(cmd)
        .then(ack => this._onAck(ack, onAck))
        .catch(error => {
          onAck.onError(new CommandHandlingError(error.message, error));
        });
  }

  _onAck(ack, onAck) {
    const responseStatus = ack.status;
    const responseStatusProto = ObjectToProto.convert(responseStatus, _statusType);
    const responseStatusCase = responseStatusProto.getStatusCase();

    switch (responseStatusCase) {
      case Status.StatusCase.OK:
        onAck.onOk();
        break;
      case Status.StatusCase.ERROR:
        const error = responseStatusProto.getError();
        const message = error.getMessage();
        onAck.onError(error.hasValidationError()
            ? new CommandValidationError(message, error)
            : new CommandHandlingError(message, error));
        break;
      case Status.StatusCase.REJECTION:
        onAck.onRejection(responseStatusProto.getRejection());
        break;
      default:
        onAck.onError(
            new SpineError(`Unknown response status case ${responseStatusCase}`)
        );
    }
  }
}
