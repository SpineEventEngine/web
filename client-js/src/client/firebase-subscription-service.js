/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import {Subscription} from 'spine-js-client-proto/spine/client/subscription_pb';

const SECOND = 1000;
const FIVE_SECONDS = 5 * SECOND;

export class FirebaseSubscriptionService {
  /**
   *
   * @param {Endpoint} endpoint
   */
  constructor(endpoint) {
    /**
     * @type {EntitySubscription[]}
     * @private
     */
    this._subscriptions = [];
    /**
     * @type {Endpoint}
     * @private
     */
    this._endpoint = endpoint;
  }

  /**
   * @param {EntitySubscription} subscription
   */
  add(subscription) {
    if (this._isRegistered(subscription)) {
      throw "This subscription is already registered in subscription service";
    }
    this._subscriptions.push(subscription);
  }

  run() {
    if (this._interval) {
      throw "The FirebaseSubscriptionService is already running";
    }
    this._interval = setInterval(() => {
      this._keepUpSubscriptions();
    }, FIVE_SECONDS);
  }

  _keepUpSubscriptions() {
    this._subscriptions.forEach(subscription => {
      const spineSubscription = subscription.internal();
      if (subscription.closed) {
        this._endpoint.cancelSubscription(spineSubscription).then(() => {
          this._removeSubscription(subscription);
        });
      } else {
        this._endpoint.keepUpSubscription(spineSubscription);
      }
    });
  }

  stop() {
    if (!this._interval) {
      throw "The FirebaseSubscriptionService was stopped when it was not running";
    }
    clearInterval(this._interval);
    this._interval = null;
  }

  _removeSubscription(subscription) {
    const index = this._subscriptions.indexOf(subscription);
    this._subscriptions.splice(index, 1);
  }

  _isRegistered(subscription) {
    const id = subscription.id();
    const exists = this._subscriptions.find(registered => registered.id() === id);
    return !!exists;
  }
}
