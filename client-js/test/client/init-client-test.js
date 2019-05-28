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

import assert from 'assert';

import * as testProtobuf from '@testProto/index';
import {init} from '@lib/index';
import {ActorProvider} from '@lib/client/actor-request-factory';
import {MockClient} from './test-helpers';
import {FirebaseClient} from '@lib/client/firebase-client';
import KnownTypes from '@lib/client/known-types';

class Given {

  constructor() {
    throw new Error('A utility Given class cannot be instantiated.');
  }

  /**
   * Options required for `FirebaseClient` initialization.
   *
   * @return {ClientOptions}
   */
  static firebaseClientOptions() {
    return {
      protoIndexFiles: Given.PROTO_FILES,
      endpointUrl: Given.ENDPOINT_URL,
      firebaseDatabase: {
        // There's no need to pass real Firebase database for these tests
      },
      actorProvider: Given.ACTOR_PROVIDER
    };
  }

  /**
   * Options required for custom client initialization.
   *
   * @return {ClientOptions}
   */
  static customClientOptions() {
    return {
      protoIndexFiles: Given.PROTO_FILES,
      implementation: Given.MOCK_CLIENT
    }
  }

  /**
   * Checks whether all types from given proto files were registered in `KnownTypes`.
   *
   * @return {boolean}
   */
  static protoFilesRegistered() {
    const protoIndexFile = Given.PROTO_FILES[0];
    const typeUrlIterator = protoIndexFile.types.keys();

    let typeUrl = typeUrlIterator.next();
    let isRegistered = true;

    while (!typeUrl.done) {
      if (!KnownTypes.hasType(typeUrl.value)) {
        isRegistered = false;
        break;
      }
      typeUrl = typeUrlIterator.next();
    }

    return isRegistered;
  }
}

Given.PROTO_FILES = [testProtobuf];
Given.ENDPOINT_URL = 'http://no-reply-server.appspot.com';
Given.ACTOR_PROVIDER = new ActorProvider();
Given.MOCK_CLIENT = new MockClient();

describe('`init` function', function () {

  it('should register types defined in proto files', () => {
    KnownTypes.clear();
    assert.ok(!Given.protoFilesRegistered());
    init(Given.customClientOptions());

    assert.ok(Given.protoFilesRegistered());
  });

  it('should create a FirebaseClient instance for the correct options', () => {
    const client = init(Given.firebaseClientOptions());
    assert.ok(client instanceof FirebaseClient);
  });

  it('should return the provided `Client` implementation for the correct options', () => {
    const client = init(Given.customClientOptions());
    assert.ok(client === Given.MOCK_CLIENT);
  });

  it('should throw an error when provided `Client` implementation does not extend `Client` ', () => {
    const optionsWithMalformedClient = Object.assign(Given.customClientOptions(), {implementation: {}});
    assert.throws(() => {
      init(optionsWithMalformedClient);
    });
  });

  it('should throw an error when proto files missing', () => {
    const optionsWithNoProtoFiles = Object.assign(Given.firebaseClientOptions(), {protoIndexFiles: []});
    assert.throws(() => {
      init(optionsWithNoProtoFiles);
    });
  });

  it('should throw an error when endpoint URL missing', () => {
    const optionsWithNoEndpointUrl = Object.assign(Given.firebaseClientOptions(), {endpointUrl: undefined});
    assert.throws(() => {
      init(optionsWithNoEndpointUrl);
    });
  });

  it('should throw an error when actor provider missing', () => {
    const optionsWithNoActorProvider = Object.assign(Given.firebaseClientOptions(), {actorProvider: undefined});
      assert.throws(() => {
        init(optionsWithNoActorProvider);
      });
  });
});
