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

import {Client} from './client';
import {HttpClient} from "./http-client";
import {HttpEndpoint} from "./http-endpoint";
import {ActorRequestFactory} from "./actor-request-factory";
import {CommandingClient} from "./composite-client";

/**
 * @typedef {Object} ClientOptions a type of object for initialization of Spine client
 *
 * @property {!Array<Object>} protoIndexFiles
 *  the list of the `index.js` files generated by {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
 * @property {?string} endpointUrl
 *  the optional URL of the Spine-based backend endpoint
 * @property {?firebase.database.Database} firebaseDatabase
 *  the optional Firebase Database that will be used to retrieve data from
 * @property {?ActorProvider} actorProvider
 *  the optional provider of the user interacting with Spine
 * @property {?Client} implementation
 *  the optional custom implementation of `Client`
 */

/**
 * An abstract factory for creation of `Client` instances.
 *
 * Ensures that the `ClientOptions` contain list of the `index.js` files generated by
 * {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
 * and performs registration of types and parsers containing in these files.
 *
 * Creation of the concrete implementation of `Client` instances is delegated to inheritors.
 */
export class AbstractClientFactory {

  /**
   * Creates a new instance of `Client` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options client initialization options
   * @return {Client} a `Client` instance
   */
  static createClient(options) {
    this._ensureOptionsSufficient(options);
    return this._clientFor(options);
  }

  /**
   * Creates a new instance of `Client` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options
   * @return {Client}
   * @protected
   */
  static _clientFor(options) {
    throw new Error('Not implemented in abstract base')
  }

  /**
   * Creates a new instance of `QueryingClient` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options client initialization options
   * @return {QueryingClient} a `QueryingClient` instance
   */
  static createQuerying(options) {
    this._ensureOptionsSufficient(options);
    return this._queryingClient(options);
  }

  /**
   * Creates a new instance of `QueryingClient` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options
   * @return {QueryingClient}
   * @protected
   */
  static _queryingClient(options) {
    throw new Error('Not implemented in abstract base')
  }

  /**
   * Creates a new instance of `SubscribingClient` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options client initialization options
   * @return {SubscribingClient} a `SubscribingClient` instance
   */
  static createSubscribing(options) {
    this._ensureOptionsSufficient(options);
    return this._subscribingClient(options);
  }

  /**
   * Creates a new instance of `SubscribingClient` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options
   * @return {SubscribingClient}
   * @protected
   */
  static _subscribingClient(options) {
    throw new Error('Not implemented in abstract base')
  }

  /**
   * Creates a new instance of `CommandingClient` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options client initialization options
   * @return {CommandingClient} a `CommandingClient` instance
   */
  static createCommanding(options) {
    this._ensureOptionsSufficient(options);
    return this._commandingClient(options);
  }

  /**
   * Creates a new instance of `CommandingClient` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options
   * @return {CommandingClient}
   * @protected
   */
  static _commandingClient(options) {
    const httpClient = new HttpClient(options.endpointUrl);
    const endpoint = new HttpEndpoint(httpClient);
    const requestFactory = new ActorRequestFactory(options.actorProvider);

    return new CommandingClient(endpoint, requestFactory);
  }

  /**
   * Ensures whether options object is sufficient for client initialization.
   *
   * @param {!ClientOptions} options
   * @protected
   */
  static _ensureOptionsSufficient(options) {
    if (!options) {
      throw new Error('Unable to initialize client. The `ClientOptions` is undefined.');
    }

    const indexFiles = options.protoIndexFiles;
    if (!Array.isArray(indexFiles) || indexFiles.length === 0) {
      throw new Error('Only a non-empty array is allowed as ClientOptions.protoIndexFiles parameter.');
    }

    indexFiles.forEach(indexFile => {
      if (typeof indexFile !== 'object'
          || !(indexFile.types instanceof Map)
          || !(indexFile.parsers instanceof Map) ) {
        throw new Error('Unable to register Protobuf index files.' +
          ' Check the `ClientOptions.protoIndexFiles` contains files' +
          ' generated with "io.spine.tools:spine-proto-js-plugin".');
      }
    });
  }
}

/**
 * An implementation of the `AbstractClientFactory` that returns a client instance
 * provided in `ClientOptions` parameter.
 */
export class CustomClientFactory extends AbstractClientFactory {

  /**
   * Returns a custom `Client` implementation provided in options. Expects that the given options
   * contain an implementation which extends `Client`.
   *
   * Can be used to provide mock implementations of `Client`.
   *
   * @param {ClientOptions} options
   * @return {Client} a custom `Client` implementation provided in options
   * @override
   */
  static _clientFor(options) {
    return options.implementation;
  }

  /**
   * Returns a custom `QueryingClient` implementation provided in options. Expects that the given
   * options contain an implementation which extends `QueryingClient`.
   *
   * Can be used to provide mock implementations of `QueryingClient`.
   *
   * @param {ClientOptions} options
   * @return {QueryingClient} a custom `QueryingClient` implementation provided in options
   * @override
   */
  static _queryingClient(options) {
    return options.implementation;
  }

  /**
   * Returns a custom `SubscribingClient` implementation provided in options. Expects that the given
   * options contain an implementation which extends `SubscribingClient`.
   *
   * Can be used to provide mock implementations of `SubscribingClient`.
   *
   * @param {ClientOptions} options
   * @return {SubscribingClient} a custom `SubscribingClient` implementation provided in options
   * @override
   */
  static _subscribingClient(options) {
    return options.implementation;
  }

  /**
   * Returns a custom `CommandingClient` implementation provided in options. Expects that the given
   * options contain an implementation which extends `CommandingClient`.
   *
   * Can be used to provide mock implementations of `CommandingClient`.
   *
   * @param {ClientOptions} options
   * @return {CommandingClient} a custom `CommandingClient` implementation provided in options
   * @override
   */
  static _commandingClient(options) {
    return options.implementation;
  }

  /**
   * @override
   */
  static _ensureOptionsSufficient(options) {
    super._ensureOptionsSufficient(options);
    const customClient = options.implementation;
    if (!customClient || !(customClient instanceof Client)) {
      throw new Error('Unable to initialize custom implementation.' +
        ' The `ClientOptions.implementation` should extend Client.');
    }
  }
}
