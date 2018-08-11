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

import {TypedMessage, TypeUrl} from "./typed-message";
import web from "spine-js-client-proto/spine/web/web_query_pb"

/**
 * The type URL representing the spine.client.Query.
 *
 * @type {TypeUrl}
 */
const WEB_QUERY_MESSAGE_TYPE = new TypeUrl("type.spine.io/spine.web.WebQuery");

/**
 * An endpoint to send Commands and Queries to.
 */
export class Endpoint {

  /**
   * @param httpClient {HttpClient} a client sending requests to server
   */
  constructor(httpClient) {
    this._httpClient = httpClient;
  }

  /**
   * Sends off a command to the endpoint.
   *
   * @param command {TypedMessage<Command>} a Command send to Spine
   * @return {Promise<Object>} a promise of a successful server response JSON data, rejected if
   *                           the client response is not 2xx
   */
  command(command) {
    return this._httpClient
      .postMessage("/command", command)
      .then(response => _jsonOrRejection(response));
  }

  /**
   * Sends off a query to the endpoint.
   *
   * @param query {TypedMessage<Query>} a Query to Spine to retrieve some domain entities
   * @param strategy {QUERY_STRATEGY} a strategy for query results delivery
   * @return {Promise<Object>} a promise of a successful server response JSON data, rejected if
   *                           the client response is not 2xx
   */
  query(query, strategy) {
    const webQuery = _newWebQuery({of: query, delivered: strategy});
    const typedQuery = new TypedMessage(webQuery, WEB_QUERY_MESSAGE_TYPE);
    return this._httpClient
      .postMessage("/query", typedQuery)
      .then(response => _jsonOrRejection(response));
  }
}

/**
 * @param response {Response} an HTTP request response
 * @return {boolean} true if the response status code is from 200 to 299, false otherwise
 * @private
 */
function _isSuccessfulResponse(response) {
  return 200 <= response.status || response.status < 300;
}

/**
 * Retrieves the response JSON data if the response was successful, returning a rejection otherwise
 *
 * @param response {Response} an HTTP request response
 * @return {Object|Promise} response JSON or rejected promise
 * @private
 */
function _jsonOrRejection(response) {
  if (_isSuccessfulResponse(response)) {
    return response.json()
  } else {
    return Promise.reject("HTTP Request failed");
  }
}

/**
 * Builds a new WebQuery from Query and client delivery strategy.
 *
 * @param of {Query} a Query to be executed by Spine
 * @param delivered {QUERY_STRATEGY}
 * @private
 */
function _newWebQuery({of: query, delivered: transactionally}) {
  const webQuery = new web.WebQuery();
  webQuery.setQuery(query);
  webQuery.setDeliveredTransactionally(transactionally);
  return webQuery;
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

