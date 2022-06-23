/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import {FilteringRequest} from "./filtering-request";

/**
 * @typedef EventSubscriptionCallbacks
 *
 * A pair of callbacks that allow to add an event consumer to the subscription and to cancel it
 * respectively.
 *
 * @property {consumerCallback<eventConsumer>} subscribe the callback which allows to setup an
 *                                                       event consumer to use for the subscription
 * @property {parameterlessCallback} unsubscribe the callback which allows to cancel the
 *                                               subscription
 */

/**
 * An abstract base for requests that subscribe to messages of a certain type.
 *
 * @abstract
 * @template <T> the target type of messages, for events the type is always `spine.core.Event`
 */
class SubscribingRequest extends FilteringRequest {

  /**
   * Builds a `Topic` instance based on the currently specified filters.
   *
   * @return {spine.client.Topic} a `Topic` instance
   */
  topic() {
    return this._builder().build();
  }

  /**
   * Posts a subscription request and returns the result as `Promise`.
   *
   * @return {Promise<EntitySubscriptionObject<Message> | EventSubscriptionObject>}
   *         the asynchronously resolved subscription object
   */
  post() {
    const topic = this.topic();
    return this._subscribe(topic);
  }

  /**
   * @inheritDoc
   */
  _newBuilderFn() {
    return requestFactory => requestFactory.topic().select(this.targetType);
  }

  /**
   * @abstract
   * @return {Promise<EntitySubscriptionObject<Message> | EventSubscriptionObject>}
   *
   * @protected
   */
  _subscribe(topic) {
    throw new Error('Not implemented in abstract base.');
  }
}

/**
 * A request to subscribe to updates of entity states of a certain type.
 *
 * Allows to obtain the `EntitySubscriptionObject` which exposes the entity changes in a form of
 * callbacks which can be subscribed to.
 *
 * A usage example:
 * ```
 * client.subscribeTo(Task.class)
 *       .where(Filters.eq("status", Task.Status.ACTIVE))
 *       // Additional filtering can be done here.
 *       .post()
 *       .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
 *           itemAdded.subscribe(_addDisplayedTask);
 *           itemChanged.subscribe(_changeDisplayedTask);
 *           itemRemoved.subscribe(_removeDisplayedTask);
 *       });
 * ```
 *
 * If the entity matched the subscription criteria at one point, but stopped to do so, the
 * `itemRemoved` callback will be triggered for it. The callback will contain the last entity state
 * that matched the subscription.
 *
 * Please note that the subscription object should be manually unsubscribed when it's no longer
 * needed to receive the updates. This can be done with the help of `unsubscribe` callback.
 *
 * @template <T> the target entity type
 */
export class SubscriptionRequest extends SubscribingRequest {

  /**
   * @param {!Class<Message>} entityType the target entity type
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
   */
  constructor(entityType, client, actorRequestFactory) {
    super(entityType, client, actorRequestFactory)
  }

  /**
   * @inheritDoc
   *
   * @return {Promise<EntitySubscriptionObject<Message>>}
   */
  _subscribe(topic) {
    return this._client.subscribe(topic);
  }

  /**
   * @inheritDoc
   */
  _self() {
    return this;
  }
}

/**
 * A request to subscribe to events of a certain type.
 *
 * Allows to obtain the `EventSubscriptionObject` which reflects the events that happened in the
 * system and match the subscription criteria.
 *
 * A usage example:
 * ```
 * client.subscribeToEvent(TaskCreated.class)
 *       .where([Filters.eq("task_priority", Task.Priority.HIGH),
 *              Filters.eq("context.past_message.actor_context.actor", userId)])
 *       .post()
 *       .then(({eventEmitted, unsubscribe}) => {
 *           eventEmitted.subscribe(_logEvent);
 *       });
 * ```
 *
 * The fields specified to the `where` filters should either be a part of the event message or
 * have a `context.` prefix and address one of the fields of the `EventContext` type.
 *
 * The `eventEmitted` observable reflects all events that occurred in the system and match the
 * subscription criteria, in a form of `spine.core.Event`.
 *
 * Please note that the subscription object should be manually unsubscribed when it's no longer
 * needed to receive the updates. This can be done with the help of `unsubscribe` callback.
 */
export class EventSubscriptionRequest extends SubscribingRequest {

  /**
   * @param {!Class<Message>} eventType the target event type
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
   */
  constructor(eventType, client, actorRequestFactory) {
    super(eventType, client, actorRequestFactory)
  }

  /**
   * @inheritDoc
   *
   * @return {Promise<EventSubscriptionObject>}
   */
  _subscribe(topic) {
    return this._client.subscribeToEvents(topic);
  }

  /**
   * @inheritDoc
   */
  _self() {
    return this;
  }
}
