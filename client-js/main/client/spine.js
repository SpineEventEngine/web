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

import {FirebaseClientFactory} from './firebase-client';
import {CustomClientFactory} from './client-factory';
import {DirectClientFactory} from "./direct-client";
import {CompositeClient} from "./composite-client";
import KnownTypes from "./known-types";
import TypeParsers from "./parser/type-parsers";

/**
 * @typedef {Object} CompositeClientOptions is a configuration of a composite client, which allows
 * different client implementations to be used for different client requests.
 *
 * @property {!Array<Object>} protoIndexFiles
 *  the list of the `index.js` files generated by {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
 * @property {ClientOptions} forQueries
 *  options of the client used for queries
 * @property {ClientOptions} forSubscriptions
 *  options of the client used for subscriptions
 * @property {ClientOptions} forCommands
 *  options of the client used for commands
 */

/**
 * The main entry point of the `spine-web` JS library. Serves for initialization
 * of the `Client` instances to interact with Spine-based backend.
 *
 * To initialize a new instance of client that uses Firebase as a storage do the following:
 * ```
 *  import * as protobufs from './proto/index.js';
 *  import * as spineWeb from 'spine-web';
 *
 *  const firebaseApp = Firebase.initializeApp({...Firebase options});
 *
 *  // The backend client will receive updates of the current actor through this instance
 *  const actorProvider = new ActorProvider();
 *
 *  const client = spineWeb.init({
 *      protoIndexFiles: [protobufs],
 *      endpointUrl: 'http://example.appspot.com',
 *      firebaseDatabase: firebaseApp.database(),
 *      actorProvider: actorProvider
 *  });
 * ```
 *
 * To substitute a custom implementation of `Client` for tests do the following:
 * ```
 *  // An instance of class extending `spineWeb.Client`
 *  const mockClientImpl = new MockClient();
 *
 *  const mockClient = spineWeb.init({
 *      protoIndexFiles: [protobufs],
 *      implementation: mockClientImpl
 *  });
 * ```
 * Note, when using of custom `Client` implementation protobuf index files
 * registration is still required.
 *
 * @param {ClientOptions|CompositeClientOptions} options
 * @return {Client}
 */
export function init(options) {
  _registerTypes(...options.protoIndexFiles);
  const compositeClient = _initCompositeClient(options);
  return compositeClient !== null ? compositeClient : _initSimpleClient(options);
}

function _initCompositeClient(options) {
  const forQueries = options.forQueries;
  const forSubscriptions = options.forSubscriptions;
  const forCommands = options.forCommands;

  if (!!forQueries || !!forSubscriptions || !!forCommands) {
    if (!(!!forQueries && !!forSubscriptions && !!forCommands)) {
      throw Error("All of `forQueries`, `forSubscriptions`, and `forCommands` must be defined.");
    }
  } else {
    return null;
  }

  const querying = _selectFactory(forQueries).createQuerying(forQueries);
  const subscribing = _selectFactory(forSubscriptions).createSubscribing(forSubscriptions);
  const commanding = _selectFactory(forCommands).createCommanding(forCommands);
  return new CompositeClient(querying, subscribing, commanding);
}

function _initSimpleClient(options) {
  const factory = _selectFactory(options);
  return factory.createClient(options);
}

function _selectFactory(options) {
  let clientFactory;
  if (!!options.firebaseDatabase) {
    clientFactory = FirebaseClientFactory;
  } else if (!!options.implementation) {
    clientFactory = CustomClientFactory;
  } else {
    clientFactory = DirectClientFactory;
  }
  return clientFactory;
}

/**
 * Registers all Protobuf types provided by the specified modules.
 *
 * After the registration, the types can be used and parsed correctly.
 *
 * @param protoIndexFiles the index.js files generated by
 * {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
 * @private
 */
function _registerTypes(...protoIndexFiles) {
  for (let indexFile of protoIndexFiles) {
    for (let [typeUrl, type] of indexFile.types) {
      KnownTypes.register(type, typeUrl);
    }
    for (let [typeUrl, parserType] of indexFile.parsers) {
      TypeParsers.register(new parserType(), typeUrl);
    }
  }
}
