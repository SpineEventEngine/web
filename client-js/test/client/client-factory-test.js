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

import assert from 'assert';
import {UserId} from '@proto/spine/core/user_id_pb';
import * as testTypes from '@testProto/index';
import * as types from '@proto/index';
import {ActorProvider} from '@lib/client/actor-request-factory';
import {init} from '@lib/client/spine';
import {Client} from "@lib/client/client";
import {CompositeClient} from "@lib/client/composite-client";

class TestClient extends Client {}

describe('Client factory should', () => {

  it('create composite client', done => {
    const endpoint = 'example.com';
    const userId = new UserId();
    userId.value = 'me';
    const client = init({
      protoIndexFiles: [types, testTypes],
      forQueries: {
        endpointUrl: `${endpoint}/q/`,
        actorProvider: new ActorProvider(userId)
      },
      forSubscriptions: {
        endpointUrl: `${endpoint}/q/`,
        actorProvider: new ActorProvider(userId),
        firebaseDatabase: "mock database"
      },
      forCommands: {
        endpointUrl: `${endpoint}/c/`,
        actorProvider: new ActorProvider(userId),
        implementation: new TestClient()
      }
    });
    assert.ok(client instanceof CompositeClient);
    assert.ok(client._commanding instanceof TestClient);
    done();
  });
});
