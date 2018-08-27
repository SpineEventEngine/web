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

import {TypedMessage, TypeUrl} from './typed-message';
import {WebQuery} from 'spine-web-client-proto/spine/web/web_query_pb';

/**
 * The type URL representing the spine.client.Query.
 *
 * @type {TypeUrl}
 */
const WEB_QUERY_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.web.WebQuery');
const SUBSCRIPTION_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.client.Subscription');

/**
 * An error which occurred when sending off a request to Spine server endpoint.
 */
export class EndpointError {
  /**
   * @param {boolean} causedByClient `true` if the error was caused by the client, `false` otherwise
   * @param {Object} reason the reason why this error occurred
   */
  constructor(causedByClient, reason) {
    this._causedByClient = causedByClient;
    this._reason = reason;
  }

  /**
   * @return {boolean} `true` if the error was caused by an invalid client behaviour
   */
  isClient() {
    return this._causedByClient;
  }

  /**
   * @return {boolean} `true` in case of the server error
   */
  isServer() {
    return !this._causedByClient;
  }

  /**
   * @return {Object} the reason of the error
   */
  reason() {
    return this._reason;
  }

  /**
   * Returns new `EndpointError` caused by the client.
   *
   * @param reason the reason why the error occurred
   * @return {EndpointError} new error instance
   */
  static clientError(reason) {
    const CAUSED_BY_CLIENT = true;
    return new EndpointError(CAUSED_BY_CLIENT, reason);
  }

  /**
   * Returns new `EndpointError` caused by the server.
   *
   * @param reason the reason why the error occurred
   * @return {EndpointError} new error instance
   */
  static serverError(reason) {
    const CAUSED_BY_SERVER = false;
    return new EndpointError(CAUSED_BY_SERVER, reason);
  }
}

class Endpoint {

  /**
   * Sends off a command to the endpoint.
   *
   * @param {!TypedMessage<Command>} command a Command send to Spine server
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  command(command) {
    return this._executeCommand(command);
  }

  /**
   * Sends off a query to the endpoint.
   *
   * @param {!Query} query a Query to Spine server to retrieve some domain entities
   * @param {!QUERY_STRATEGY} strategy a strategy for query results delivery
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  query(query, strategy) {
    const webQuery = Endpoint._newWebQuery({of: query, delivered: strategy});
    const typedQuery = new TypedMessage(webQuery, WEB_QUERY_MESSAGE_TYPE);
    return this._performQuery(typedQuery);
  }

  /**
   * Sends off a request to subscribe to a provided topic to an endpoint.
   *
   * @param {!Topic} topic a topic for which a subscription is created
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  subscribeTo(topic) {
    const typedTopic = new TypedMessage(topic, new TypeUrl('type.spine.io/spine.client.Topic'));
    return this._subscribeTo(typedTopic);
  }

  /**
   * Sends off a request to keep a subscription, stopping it from being closed by server.
   *
   * @param {!spine.client.Subscription} subscription a subscription that should be kept open
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  keepUpSubscription(subscription) {
    const typedSubscription = new TypedMessage(subscription, SUBSCRIPTION_MESSAGE_TYPE);
    return this._keepUp(typedSubscription);
  }

  /**
   * Sends off a request to cancel an existing subscription.
   *
   * @param {!spine.client.Subscription} subscription a subscription that should be kept open
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  cancelSubscription(subscription) {
    const typedSubscription = new TypedMessage(subscription, SUBSCRIPTION_MESSAGE_TYPE);
    return this._cancel(typedSubscription);
  }

  /**
   * Builds a new WebQuery from Query and client delivery strategy.
   *
   * @param {!Query} of a Query to be executed by Spine server
   * @param {!QUERY_STRATEGY} delivered the strategy for query results delivery
   * @private
   */
  static _newWebQuery({of: query, delivered: transactionally}) {
    const webQuery = new WebQuery();
    webQuery.setQuery(query);
    webQuery.setDeliveredTransactionally(transactionally);
    return webQuery;
  }

  /**
   * @param {!TypedMessage<Command>} command a Command send to Spine server
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _executeCommand(command) {
    throw 'Not implemented in abstract base.';
  }

  /**
   * @param {!TypedMessage<WebQuery>} query a Query to Spine server to retrieve some domain entities
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _performQuery(query) {
    throw 'Not implemented in abstract base.';
  }

  /**
   * @param {!TypedMessage<Topic>} topic a topic to create a subscription for
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _subscribeTo(topic) {
    throw 'Not implemented in abstract base.';
  }

  /**
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription to keep alive
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _keepUp(subscription) {
    throw 'Not implemented in abstract base.';
  }

  /**
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription to be canceled
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   * @protected
   * @abstract
   */
  _cancel(subscription) {
    throw 'Not implemented in abstract base.';
  }
}

/**
 * Spine HTTP endpoint which is used to send off Commands and Queries using
 * the provided HTTP client.
 */
export class HttpEndpoint extends Endpoint {

  /**
   * @param {!HttpClient} httpClient a client sending requests to server
   */
  constructor(httpClient) {
    super();
    this._httpClient = httpClient;
  }

  /**
   * Sends off a command to the endpoint.
   *
   * @param {!TypedMessage<Command>} command a Command send to Spine server
   * @return {Promise<Object>} a promise of a successful server response JSON data, rejected if
   *                           the client response is not 2xx
   * @protected
   */
  _executeCommand(command) {
    return this._httpClient
      .postMessage('/command', command)
      .then(HttpEndpoint._jsonOrRejection);
  }

  /**
   * Sends off a query to the endpoint.
   *
   * @param {!TypedMessage<WebQuery>} webQuery a Query to Spine server to retrieve some domain entities
   * @param {!QUERY_STRATEGY} strategy a strategy for query results delivery
   * @return {Promise<Object>} a promise of a successful server response JSON data, rejected if
   *                           the client response is not 2xx
   * @protected
   */
  _performQuery(webQuery) {
    return this._httpClient
      .postMessage('/query', webQuery)
      .then(HttpEndpoint._jsonOrRejection);
  }

  /**
   * Sends off a request to create a subscription for a topic.
   *
   * @param {!TypedMessage<Topic>} topic a topic to subscribe to
   * @return {Promise<Response>} a promise of a successful server response JSON data, rejected if
   *                             the client response is not 2xx
   * @protected
   */
  _subscribeTo(topic) {
    return this._httpClient
      .postMessage('/subscription/create', topic)
      .then(HttpEndpoint._jsonOrRejection);
  }

  /**
   * Sends off a request to create a subscription for a topic.
   *
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription that is prevented 
 *                                                                  from being closed by server
   * @return {Promise<Response>} a promise of a successful server response JSON data, rejected if
   *                             the client response is not 2xx
   * @protected
   */
  _keepUp(subscription) {
    return this._httpClient
      .postMessage('/subscription/keep-up', subscription)
      .then(HttpEndpoint._jsonOrRejection);
  }

  _cancel(subscription) {
    return this._httpClient
      .postMessage('/subscription/cancel', subscription)
      .then(HttpEndpoint._jsonOrRejection);
  }

  /**
   * Retrieves the response JSON data if the response was successful, returning a rejection otherwise
   *
   * @param {!Response} response an HTTP request response
   * @return {Object|Promise} response JSON or rejected promise
   * @private
   */
  static _jsonOrRejection(response) {
    if (HttpEndpoint._isSuccessfulResponse(response)) {
      return response.json();
    } else {
      if (400 <= response.status && response.status < 500) {
        return Promise.reject(EndpointError.clientError(response));
      } else {
        return Promise.reject(EndpointError.serverError(response));
      }
    }
  }

  /**
   * @param {!Response} response an HTTP request response
   * @return {boolean} `true` if the response status code is from 200 to 299, `false` otherwise
   * @private
   */
  static _isSuccessfulResponse(response) {
    return 200 <= response.status && response.status < 300;
  }
}

/**
 * Enum of WebQuery transactional delivery attribute values.
 *
 * Specifies the strategy for delivering Query results, sending message to client all-at-once or
 * one-by-one.
 *
 * @readonly
 * @enum boolean
 */
export const QUERY_STRATEGY = Object.freeze({
  allAtOnce: true,
  oneByOne: false
});
