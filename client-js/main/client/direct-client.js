/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import {ActorRequestFactory} from './actor-request-factory';
import {AnyPacker} from "./any-packer";
import {AbstractClientFactory} from './client-factory';
import {CommandingClient} from "./commanding-client";
import {CompositeClient} from "./composite-client";
import {HttpClient} from './http-client';
import {HttpEndpoint} from './http-endpoint';
import KnownTypes from "./known-types";
import {QueryingClient} from "./querying-client";
import {NoOpSubscribingClient} from "./subscribing-client";
import {Type} from "./typed-message";
import TypeParsers from "./parser/type-parsers";

/**
 * An implementation of the `AbstractClientFactory` that creates instances of client which exchanges
 * data with the server directly.
 *
 * Querying is performed by sending a query to the server over HTTP and reading the query response
 * from the HTTP response.
 *
 * This client does not support subscriptions.
 */
export class DirectClientFactory extends AbstractClientFactory {

  static _clientFor(options) {
    const httpClient = new HttpClient(options.endpointUrl);
    const endpoint = new HttpEndpoint(httpClient, options.routing);
    const requestFactory = new ActorRequestFactory(options.actorProvider);

    const querying = new DirectQueryingClient(endpoint, requestFactory);
    const subscribing = new NoOpSubscribingClient(requestFactory);
    const commanding = new CommandingClient(endpoint, requestFactory);
    return new CompositeClient(querying, subscribing, commanding);
  }

  static createQuerying(options) {
    const httpClient = new HttpClient(options.endpointUrl);
    const endpoint = new HttpEndpoint(httpClient, options.routing);
    const requestFactory = new ActorRequestFactory(options.actorProvider);

    return new DirectQueryingClient(endpoint, requestFactory);
  }

  static createSubscribing(options) {
    const requestFactory = new ActorRequestFactory(options.actorProvider);
    return new NoOpSubscribingClient(requestFactory);
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

/**
 * A {@link QueryingClient} which reads entity states directly from the server.
 */
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

  read(query) {
    const typeUrl = query.getTarget().getType();
    const targetClass = KnownTypes.classFor(typeUrl);
    const targetType = Type.of(targetClass, typeUrl);
    const responseParser = TypeParsers.parserFor('type.spine.io/spine.client.QueryResponse');
    return this._endpoint
        .query(query)
        .then(response => {
          const message = responseParser.fromObject(response);
          const entityStates = message.getMessageList();
          return entityStates.map(entity => DirectQueryingClient._unpack(entity, targetType));
        });
  }

  static _unpack(entity, targetType) {
    const unpacker = AnyPacker.unpack(entity.getState());
    return unpacker.as(targetType);
  }
}
