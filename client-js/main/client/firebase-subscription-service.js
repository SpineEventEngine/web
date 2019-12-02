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

import {Duration} from './time-utils';
import ObjectToProto from "./object-to-proto";
import {Status} from '../proto/spine/core/response_pb';


const SUBSCRIPTION_KEEP_UP_INTERVAL = new Duration({minutes: 2});

/**
 * A service that manages the active subscriptions periodically sending requests to keep them
 * running.
 */
export class FirebaseSubscriptionService {
  /**
   * @param {Endpoint} endpoint an endpoint to communicate with
   */
  constructor(endpoint) {
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
  }

  /**
   * Starts the subscription service, keeping up the added subscriptions.
   */
  run() {
    if (this._interval) {
      throw new Error('The FirebaseSubscriptionService is already running');
    }

    this._interval = setInterval(() => {
      this._keepUpSubscriptions();
    }, SUBSCRIPTION_KEEP_UP_INTERVAL.inMs());
  }

  /**
   * Sends the "keep-up" request for all active subscriptions.
   *
   * The non-`OK` response status on this request means the subscription has already been canceled
   * by the server, most likely due to a timeout. So, in such case, the subscription is removed
   * from the list of active subscriptions.
   *
   * @private
   */
  _keepUpSubscriptions() {
    this._subscriptions.forEach(subscription => {
      const spineSubscription = subscription.internal();
      if (subscription.closed) {
        this._endpoint.cancelSubscription(spineSubscription).then(() => {
          this._removeSubscription(subscription);
        });
      } else {
        this._endpoint.keepUpSubscription(spineSubscription).then(response => {
          const responseStatus = response.status;
          const responseStatusProto = ObjectToProto.convert(responseStatus, _statusType);
          if (responseStatusProto.getStatusCase() !== Status.StatusCase.OK) {
            this._removeSubscription(subscription)
          }
        });
      }
    });
  }

  /**
   * Stops the subscription service unsubscribing and removing all added subscriptions.
   */
  stop() {
    if (!this._interval) {
      throw new Error('The FirebaseSubscriptionService was stopped when it was not running');
    }
    clearInterval(this._interval);
    this._subscriptions.forEach(subscription => {
      subscription.unsubscribe();
      this._removeSubscription(subscription);
    });
    this._interval = null;
  }

  /**
   * Removes the provided subscription from subscriptions list, which stops any attempts
   * to update it.
   *
   * @private
   */
  _removeSubscription(subscription) {
    const index = this._subscriptions.indexOf(subscription);
    this._subscriptions.splice(index, 1);
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
