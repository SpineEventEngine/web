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

import {FirebaseClientFactory} from './firebase-client';
import {CustomClientFactory} from './client-factory';

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
 * @param {ClientOptions} options
 * @return {Client}
 */
export function init(options) {
  let clientFactory;

  if (!!options.firebaseDatabase) {
    clientFactory = FirebaseClientFactory;
  } else {
    clientFactory = CustomClientFactory;
  }

  return clientFactory.createClient(options);
}
