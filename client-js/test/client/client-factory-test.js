/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import {HttpClient} from "../../main/client/http-client";
import {HttpResponseHandler} from "../../main/client/http-response-handler";

class TestClient extends Client {}

class QueryHttpClient extends HttpClient {}

class SubscriptionHttpClient extends HttpClient {}

class CommandHttpClient extends HttpClient {}

class QueryResponseHandler extends HttpResponseHandler {}

class SubscriptionResponseHandler extends HttpResponseHandler {}

class CommandResponseHandler extends HttpResponseHandler {}

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

  it('allow to customize `HttpClient`', done => {
    const endpoint = 'example.com';
    const userId = new UserId();
    userId.value = 'me';
    const queryEndpoint = `${endpoint}/q/`;
    const subscriptionEndpoint = `${endpoint}/s/`;
    const commandEndpoint = `${endpoint}/c/`;
    const client = init({
      protoIndexFiles: [types, testTypes],
      forQueries: {
        endpointUrl: queryEndpoint,
        actorProvider: new ActorProvider(userId),
        httpClient: new QueryHttpClient(queryEndpoint)
      },
      forSubscriptions: {
        endpointUrl: subscriptionEndpoint,
        actorProvider: new ActorProvider(userId),
        firebaseDatabase: "mock database",
        httpClient: new SubscriptionHttpClient(subscriptionEndpoint)
      },
      forCommands: {
        endpointUrl: commandEndpoint,
        actorProvider: new ActorProvider(userId),
        httpClient: new CommandHttpClient(commandEndpoint)
      }
    });
    assert.ok(client._commanding._endpoint._httpClient instanceof CommandHttpClient);
    assert.strictEqual(client._commanding._endpoint._httpClient._appBaseUrl, commandEndpoint,);

    assert.ok(client._subscribing._endpoint._httpClient instanceof SubscriptionHttpClient);
    assert.strictEqual(client._subscribing._endpoint._httpClient._appBaseUrl, subscriptionEndpoint);

    assert.ok(client._querying._endpoint._httpClient instanceof QueryHttpClient);
    assert.strictEqual(client._querying._endpoint._httpClient._appBaseUrl, queryEndpoint);
    done();
  })

  it('allow to customize `HttpResponseHandler`', done => {
    const endpoint = 'example.com';
    const userId = new UserId();
    userId.value = 'me';
    const client = init({
      protoIndexFiles: [types, testTypes],
      forQueries: {
        endpointUrl: endpoint,
        actorProvider: new ActorProvider(userId),
        httpResponseHandler: new QueryResponseHandler()
      },
      forSubscriptions: {
        endpointUrl: endpoint,
        actorProvider: new ActorProvider(userId),
        firebaseDatabase: "mock database",
        httpResponseHandler: new SubscriptionResponseHandler()
      },
      forCommands: {
        endpointUrl: endpoint,
        actorProvider: new ActorProvider(userId),
        httpResponseHandler: new CommandResponseHandler()
      }
    });
    assert.ok(client._commanding._endpoint._responseHandler instanceof CommandResponseHandler);
    assert.ok(client._subscribing._endpoint._responseHandler instanceof SubscriptionResponseHandler);
    assert.ok(client._querying._endpoint._responseHandler instanceof QueryResponseHandler);
    done();
  })
});
