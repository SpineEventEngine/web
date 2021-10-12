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

import {TypedMessage} from './typed-message';
import {ClientError, ConnectionError, ServerError, SpineError} from './errors';
import {Subscriptions} from '../proto/spine/web/keeping_up_pb';

/**
 * @typedef {Object} SubscriptionRouting
 *
 * @property {string} create
 *  the name of the subscription creation endpoint; defaults to "/subscription/create"
 * @property {string} keepUp
 *  the name of the subscription keep up endpoint; defaults to "/subscription/keep-up"
 * @property {string} keepUpAll
 *  the name of the subscription bulk keep up endpoint; defaults to "/subscription/keep-up-all"
 * @property {string} cancel
 *  the name of the subscription cancellation endpoint; defaults to "/subscription/cancel"
 */

/**
 * @typedef {Object} Routing
 *
 * @property {string} query
 *  the name of the query endpoint; defaults to "/query"
 * @property {string} command
 *  the name of the command endpoint; defaults to "/command"
 * @property {!SubscriptionRouting} subscription
 *  the config of the subscription endpoints
 */

class Endpoint {

  /**
   * Sends off a command to the endpoint.
   *
   * @param {!TypedMessage<Command>} command a Command  to send to the Spine server
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  command(command) {
    return this._executeCommand(command);
  }

  /**
   * Sends off a query to the endpoint.
   *
   * @param {!spine.client.Query} query a Query to Spine server to retrieve some domain entities
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  query(query) {
    const typedQuery = TypedMessage.of(query);
    return this._performQuery(typedQuery);
  }

  /**
   * Sends off a request to subscribe to a provided topic to an endpoint.
   *
   * @param {!spine.client.Topic} topic a topic for which a subscription is created
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  subscribeTo(topic) {
    const typedTopic = TypedMessage.of(topic);
    return this._subscribeTo(typedTopic);
  }

  /**
   * Sends a request to keep a number of subscriptions, stopping them from being closed by
   * the server.
   *
   * @param {!Array<spine.client.Subscription>} subscriptions a subscription that should be kept open
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  keepUpSubscriptions(subscriptions) {
    return this._keepUpAll(subscriptions);
  }

  /**
   * Sends off a request to cancel an existing subscription.
   *
   * Cancelling subscription stops the server updating subscription with new values.
   *
   * @param {!spine.client.Subscription} subscription a subscription that should be kept open
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  cancelSubscription(subscription) {
    const typedSubscription = TypedMessage.of(subscription);
    return this._cancel(typedSubscription);
  }


  /**
   * @param {!TypedMessage<Command>} command a Command to send to the Spine server
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _executeCommand(command) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!TypedMessage<Query>} query a Query to Spine server to retrieve some domain entities
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _performQuery(query) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!TypedMessage<spine.client.Topic>} topic a topic to create a subscription for
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _subscribeTo(topic) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription to keep alive
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _keepUp(subscription) {
    throw new Error('Not implemented in abstract base.');
  }


  /**
   * @param {!Array<TypedMessage<spine.client.Subscription>>} subscriptions subscriptions to keep up
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _keepUpAll(subscriptions) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription to be canceled
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _cancel(subscription) {
    throw new Error('Not implemented in abstract base.');
  }
}

/**
 * Spine HTTP endpoint which is used to send off Commands and Queries using
 * the provided HTTP client.
 */
export class HttpEndpoint extends Endpoint {

  /**
   * @param {!HttpClient} httpClient a client sending requests to server
   * @param {Routing} routing endpoint routing parameters
   */
  constructor(httpClient, routing) {
    super();
    this._httpClient = httpClient;
    this._routing = routing;
  }

  /**
   * Sends off a command to the endpoint.
   *
   * @param {!TypedMessage<Command>} command a Command to send to the Spine server
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data,
   *                                      rejected if the client response is not 2xx or a
   *                                      connection error occurs
   * @protected
   */
  _executeCommand(command) {
    const path = (this._routing && this._routing.command) || '/command';
    return this._sendMessage(path, command);
  }

  /**
   * Sends off a query to the endpoint.
   *
   * @param {!TypedMessage<Query>} query a Query to Spine server to retrieve some domain entities
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data,
   *                                      rejected if the client response is not 2xx or a
   *                                      connection error occurs
   * @protected
   */
  _performQuery(query) {
    const path = (this._routing && this._routing.query) || '/query';
    return this._sendMessage(path, query);
  }

  /**
   * Sends off a request to create a subscription for a topic.
   *
   * @param {!TypedMessage<spine.client.Topic>} topic a topic to subscribe to
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data,
   *                                      rejected if the client response is not 2xx or a
   *                                      connection error occurs
   * @protected
   */
  _subscribeTo(topic) {
    const path = (this._routing && this._routing.subscription && this._routing.subscription.create)
        || '/subscription/create';
    return this._sendMessage(path, topic);
  }

  /**
   * Sends off a request to keep alive a subscription.
   *
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription that is prevented
   *                                                                  from being closed by server
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data,
   *                                      rejected if the client response is not 2xx or a
   *                                      connection error occurs
   * @protected
   */
  _keepUp(subscription) {
    const path = (this._routing && this._routing.subscription && this._routing.subscription.keepUp)
        || '/subscription/keep-up';
    return this._sendMessage(path, subscription);
  }


  /**
   * Sends off a request to keep alive given subscriptions.
   *
   * @param {!Array<spine.client.Subscription>} subscriptions subscriptions that are prevented
   *                                                          from being closed by the server
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data,
   *                                      rejected if the client response is not 2xx or a
   *                                      connection error occurs
   * @protected
   */
  _keepUpAll(subscriptions) {
    const path = (this._routing && this._routing.subscription && this._routing.subscription.keepUpAll)
        || '/subscription/keep-up-all';
    const request = new Subscriptions()
    request.setSubscriptionList(subscriptions);
    const typed = TypedMessage.of(request);
    return this._sendMessage(path, typed);
  }

  /**
   * Sends off a request to cancel a subscription.
   *
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription to be canceled
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data,
   *                                      rejected if the client response is not 2xx or a
   *                                      connection error occurs
   * @protected
   */
  _cancel(subscription) {
    const path = (this._routing && this._routing.subscription && this._routing.subscription.cancel)
        || '/subscription/cancel';
    return this._sendMessage(path, subscription);
  }

  /**
   * Sends the given message to the given endpoint.
   *
   * @param {!string} endpoint an endpoint to send the message to
   * @param {!TypedMessage} message a message to send, as a {@link TypedMessage}
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data,
   *                                      rejected if the client response is not 2xx or a
   *                                      connection error occurs
   * @private
   */
  _sendMessage(endpoint, message) {
    return new Promise((resolve, reject) => {
      this._httpClient
        .postMessage(endpoint, message)
        .then(HttpEndpoint._jsonOrError, HttpEndpoint._connectionError)
        .then(resolve, reject);
    });
  }

  /**
   * Retrieves the JSON data from the given response if it was successful, rejects
   * with a respective error otherwise.
   *
   * @param {!Response} response an HTTP request response
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data,
   *                                      rejected if the client response is not 2xx or if JSON
   *                                      parsing fails
   * @private
   */
  static _jsonOrError(response) {
    const statusCode = response.status;
    if (HttpEndpoint._isSuccessfulResponse(statusCode)) {
      return HttpEndpoint._parseJson(response);
    }
    else if (HttpEndpoint._isClientErrorResponse(statusCode)) {
      return Promise.reject(new ClientError(response.statusText, response));
    }
    else if(HttpEndpoint._isServerErrorResponse(statusCode)) {
      return Promise.reject(new ServerError(response));
    }
  }

  /**
   * Parses the given response JSON data, rejects if parsing fails.
   *
   * @param {!Response} response an HTTP request response
   * @return {Promise<Object|SpineError>} a promise of a server response parsing to be fulfilled
   *                                      with a JSON data or rejected with {@link SpineError} if
   *                                      JSON parsing fails.
   * @private
   */
  static _parseJson(response) {
   return response.json()
            .then(json => Promise.resolve(json))
            .catch(error => Promise.reject(new SpineError('Failed to parse response JSON', error)));
  }

  /**
   * Gets the error caught from the {@link HttpClient#postMessage} and returns
   * a rejected promise with a given error wrapped into {@link ConnectionError}.
   *
   * @param {!Error} error              an error which occurred upon message sending
   * @return {Promise<ConnectionError>} a rejected promise with a `ConnectionError`
   * @private
   */
  static _connectionError(error) {
    return Promise.reject(new ConnectionError(error));
  }

  /**
   * @param {!number} statusCode an HTTP request response status code
   * @return {boolean} `true` if the response status code is from 200 to 299, `false` otherwise
   * @private
   */
  static _isSuccessfulResponse(statusCode) {
    return 200 <= statusCode && statusCode < 300;
  }

  /**
   * @param {!number} statusCode an HTTP request response status code
   * @return {boolean} `true` if the response status code is from 400 to 499, `false` otherwise
   * @private
   */
  static _isClientErrorResponse(statusCode) {
    return 400 <= statusCode && statusCode < 500;
  }

  /**
   * @param {!number} statusCode an HTTP request response status code
   * @return {boolean} `true` if the response status code is from 500, `false` otherwise
   * @private
   */
  static _isServerErrorResponse(statusCode) {
    return 500 <= statusCode;
  }
}
