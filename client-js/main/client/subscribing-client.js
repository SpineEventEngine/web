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

import {EventSubscriptionRequest, SubscriptionRequest} from "./subscribing-request";

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
   * @param {!Class<Message>} type the target entity type
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
   * @return {Promise<EntitySubscriptionObject<Message>>} a subscription object
   *
   * @template <T> a Protobuf type of entities being the target of a subscription
   */
  subscribe(topic) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a new event subscription request.
   *
   * @param {!Class<Message>} type the target event type
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
