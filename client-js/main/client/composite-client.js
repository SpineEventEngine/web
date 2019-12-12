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

import {Client} from "./client";

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
