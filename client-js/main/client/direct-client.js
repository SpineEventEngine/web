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

import {AbstractClientFactory} from './client-factory';
import {HttpClient} from './http-client';
import {HttpEndpoint} from './http-endpoint';
import {ActorRequestFactory} from './actor-request-factory';
import {CommandingClient, CompositeClient, QueryingClient} from "./composite-client";
import KnownTypes from "./known-types";
import {AnyPacker} from "./any-packer";
import {Type} from "./typed-message";

class DirectQueryingClient extends QueryingClient {

  /**
   * @param {!HttpEndpoint} endpoint the server endpoint to execute queries and commands
   * @param {!ActorRequestFactory} actorRequestFactory a factory to instantiate the actor requests with
   *
   * @protected use `FirebaseClient#usingFirebase()` for instantiation
   */
  constructor(endpoint, actorRequestFactory) {
    super(actorRequestFactory);
    this._endpoint = endpoint;
  }

  execute(query) {
    const typeUrl = query.getTarget().getType();
    const targetClass = KnownTypes.classFor(typeUrl);
    const targetType = Type.of(targetClass, typeUrl);
    return new Promise((resolve, reject) => {
      this._endpoint.query(query)
          .then((response) => {
            const messages = response.message
                                     .map(entity => AnyPacker.unpack(entity.state).as(targetType));
            resolve(messages);
          })
          .catch(error => reject(error));
    });
  }
}

/**
 * An implementation of the `AbstractClientFactory` that creates instances of client which exchanges
 * data with the server directly.
 */
export class DirectClientFactory extends AbstractClientFactory {

  /**
   * Creates a new `FirebaseClient` instance which will send the requests on behalf of the provided
   * actor to the provided endpoint, retrieving the data from the provided Firebase storage.
   *
   * Expects that given options contain backend endpoint URL, firebase Database instance and
   * the actor provider.
   *
   * @param {ClientOptions} options
   * @return {Client} a new backend client instance which will send the requests on behalf
   *                  of the provided actor to the provided endpoint, retrieving the data
   *                  from the provided Firebase storage
   * @override
   */
  static _clientFor(options) {
    const httpClient = new HttpClient(options.endpointUrl);
    const endpoint = new HttpEndpoint(httpClient);
    const requestFactory = new ActorRequestFactory(options.actorProvider);

    const querying = new DirectQueryingClient(endpoint, requestFactory);
    const subscribing = new SubscribingClient(requestFactory);
    const commanding = new CommandingClient(endpoint, requestFactory);
    return new CompositeClient(querying, subscribing, commanding);
  }

  static _queryingClient(options) {
    const httpClient = new HttpClient(options.endpointUrl);
    const endpoint = new HttpEndpoint(httpClient);
    const requestFactory = new ActorRequestFactory(options.actorProvider);

    return new DirectQueryingClient(endpoint, requestFactory);
  }

  static _subscribingClient(options) {
    const requestFactory = new ActorRequestFactory(options.actorProvider);
    return new SubscribingClient(requestFactory);
  }

  /**
   * @override
   */
  static _ensureOptionsSufficient(options) {
    super._ensureOptionsSufficient(options);
    const messageForMissing = (option) =>
        `Unable to initialize a direct client. The ClientOptions.${option} not specified.`;
    if (!options.endpointUrl) {
      throw new Error(messageForMissing('endpointUrl'));
    }
    if (!options.actorProvider) {
      throw new Error(messageForMissing('actorProvider'));
    }
  }
}
