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

import {Type, TypedMessage} from './typed-message';
import {
  SpineError,
  RequestProcessingError,
  ServerError,
  ConnectionError
} from './errors';
import {WebQuery} from 'spine-web-client-proto/spine/web/web_query_pb';

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
   * @param {!spine.client.Query} query a Query to Spine server to retrieve some domain entities
   * @param {!QUERY_STRATEGY} strategy a strategy for query results delivery
   * @return {Promise<Object>} a promise of a successful server response, rejected if
   *                           an error occurs
   */
  query(query, strategy) {
    const webQuery = Endpoint._newWebQuery({of: query, delivered: strategy});
    const typedQuery = new TypedMessage(webQuery, Type.WEB_QUERY);
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
    const typedTopic = new TypedMessage(topic, Type.TOPIC);
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
    const typedSubscription = new TypedMessage(subscription, Type.SUBSCRIPTION);
    return this._keepUp(typedSubscription);
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
    const typedSubscription = new TypedMessage(subscription, Type.SUBSCRIPTION);
    return this._cancel(typedSubscription);
  }

  /**
   * Builds a new WebQuery from Query and client delivery strategy.
   *
   * @param {!spine.client.Query} of a Query to be executed by Spine server
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
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!TypedMessage<WebQuery>} query a Query to Spine server to retrieve some domain entities
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
   */
  constructor(httpClient) {
    super();
    this._httpClient = httpClient;
  }

  /**
   * Sends off a command to the endpoint.
   *
   * @param {!TypedMessage<Command>} command a Command send to Spine server
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data, rejected if
   *                                      the client response is not 2xx or a connection error occurs
   * @protected
   */
  _executeCommand(command) {
    return this._sendMessage('/command', command);
  }

  /**
   * Sends off a query to the endpoint.
   *
   * @param {!TypedMessage<WebQuery>} webQuery a Query to Spine server to retrieve some domain entities
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data, rejected if
   *                                      the client response is not 2xx or a connection error occurs
   * @protected
   */
  _performQuery(webQuery) {
    return this._sendMessage('/query', webQuery);
  }

  /**
   * Sends off a request to create a subscription for a topic.
   *
   * @param {!TypedMessage<spine.client.Topic>} topic a topic to subscribe to
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data, rejected if
   *                                      the client response is not 2xx or a connection error occurs
   * @protected
   */
  _subscribeTo(topic) {
    return this._sendMessage('/subscription/create', topic);
  }

  /**
   * Sends off a request to keep alive a subscription.
   *
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription that is prevented
   *                                                                  from being closed by server
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data, rejected if
   *                                      the client response is not 2xx or a connection error occurs
   * @protected
   */
  _keepUp(subscription) {
    return this._sendMessage('/subscription/keep-up', subscription);
  }

  /**
   * Sends off a request to cancel a subscription.
   *
   * @param {!TypedMessage<spine.client.Subscription>} subscription a subscription to be canceled
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data, rejected if
   *                                      the client response is not 2xx or a connection error occurs
   * @protected
   */
  _cancel(subscription) {
    return this._sendMessage('/subscription/cancel', subscription);
  }

  /**
   * Sends the given message to the given endpoint.
   *
   * @param {!string} endpoint an endpoint to send the message to
   * @param {!TypedMessage} message a message to send, as a {@link TypedMessage}
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data, rejected if
   *                                      the client response is not 2xx or a connection error occurs
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
   * @return {Promise<Object|SpineError>} a promise of a successful server response JSON data, rejected if
   *                                      the client response is not 2xx or if JSON parsing fails
   * @private
   */
  static _jsonOrError(response) {
    const statusCode = response.status;
    if (HttpEndpoint._isSuccessfulResponse(statusCode)) {
      return HttpEndpoint._parseJson(response);
    }
    else if (HttpEndpoint._isClientErrorResponse(statusCode)) {
      return Promise.reject(new RequestProcessingError(response));
    }
    else if(HttpEndpoint._isServerErrorResponse(statusCode)) {
      return Promise.reject(new ServerError(response));
    }
  }

  /**
   * Parses the given response JSON data, rejects if parsing fails.
   *
   * @param {!Response} response an HTTP request response
   * @return {Promise<Object|SpineError>} a promise of a server response parsing to be fulfilled with a JSON
   *                                      data or rejected with {@link SpineError} if JSON parsing fails.
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
