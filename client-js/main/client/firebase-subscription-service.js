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

"use strict";

import {Duration} from './time-utils';
import ObjectToProto from './object-to-proto';
import {Status} from '../proto/spine/core/response_pb';

/**
 * The default interval for sending subscription keep up requests.
 *
 * @type {Duration}
 */
const DEFAULT_KEEP_UP_INTERVAL = new Duration({minutes: 2});

/**
 * A service that manages the active subscriptions periodically sending requests to keep them
 * running.
 */
export class FirebaseSubscriptionService {

  /**
   * @param {Endpoint} endpoint an endpoint to communicate with
   * @param {?Duration} keepUpInterval a custom interval for sending subscription keep up requests
   */
  constructor(endpoint, keepUpInterval) {
    /**
     * @type {SpineSubscription[]}
     * @private
     */
    this._subscriptions = [];
    /**
     * @type {Endpoint}
     * @private
     */
    this._endpoint = endpoint;
    /**
     * @type {Duration}
     * @private
     */
    this._keepUpInterval = keepUpInterval
        ? keepUpInterval
        : DEFAULT_KEEP_UP_INTERVAL;
  }

  /**
   * Add a subscription to the service to handle the keep-up requests and cancel in
   * case of unsubscribe.
   *
   * @param {SpineSubscription} subscription an active subscription to keep running
   */
  add(subscription) {
    if (this._isRegistered(subscription)) {
      throw new Error('This subscription is already registered in subscription service');
    }
    this._subscriptions.push(subscription);

    if (!this._isRunning()) {
      this._run();
    }
  }

  /**
   * Indicates whether this service is running keeping up subscriptions.
   *
   * @returns {boolean}
   * @private
   */
  _isRunning() {
     return !!this._interval;
  }

  /**
   * Starts the subscription service, keeping up the added subscriptions.
   *
   * @private
   */
  _run() {
    this._interval = setInterval(() => {
      this._keepUpSubscriptions();
    }, this._keepUpInterval.inMs());
  }

  /**
   * Stops the subscription service.
   *
   * @private
   */
  _stop() {
    clearInterval(this._interval);
    this._interval = null;
  }

  /**
   * Sends the "keep-up" request for all active subscriptions.
   *
   * The non-`OK` response status means the subscription has already been canceled on the server,
   * most likely due to a timeout. So, in such case, the subscription is removed from the list of
   * active ones.
   *
   * @private
   */
  _keepUpSubscriptions() {
    const cancelledSubscriptions = this._subscriptions.filter(s => s.closed);
    if (cancelledSubscriptions.length > 0) {
      const subscriptionMessages = cancelledSubscriptions.map(s => s.internal())
      this._endpoint.cancelAll(subscriptionMessages);
      cancelledSubscriptions.forEach(s => this._removeSubscription(s))
    }
    const subscriptions = this._subscriptions.map(value => value.internal());
    if (subscriptions.length === 0) {
      return;
    }
    this._endpoint.keepUpSubscriptions(subscriptions).then(response => {
      for (let i = 0; i < response.response.length; i++) {
        const r = response.response[i];
        const status = ObjectToProto.convert(r.status, Status.typeUrl());
        if (status.getStatusCase() !== Status.StatusCase.OK) {
          this._removeSubscription(subscriptions[i])
        }
      }
    });
  }

  /**
   * Removes the provided subscription from subscriptions list, which stops any attempts
   * to update it. In case no more subscriptions are left, stops this service.
   *
   * @private
   */
  _removeSubscription(subscription) {
    const index = this._subscriptions.indexOf(subscription);
    this._subscriptions.splice(index, 1);

    if (this._subscriptions.length === 0) {
      this._stop();
    }
  }

  /**
   * @private
   */
  _isRegistered(subscription) {
    const id = subscription.id();
    const exists = this._subscriptions.find(registered => registered.id() === id);
    return !!exists;
  }
}
