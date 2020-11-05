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

import {firebaseDatabase} from "./firebase-database";
import * as testProtobuf from '@testProto/index';
import {ActorProvider, Client as FirebaseClient, init as initSpineWeb, TenantProvider} from '@lib';
import TestEnvironment from "../../given/test-environment";

/**
 * Initializes the {@link FirebaseClient client} that interacts with Gretty-based
 * local backend server and the emulated Firebase application.
 * See `integration-tests/README.MD` for details.
 *
 * @param {!string} endpointUrl the URL of a backend to interact with
 * @param {?TenantProvider} tenantProvider the tenant provider for multitenant context tests
 * @param {?Duration} keepUpInterval the custom interval for sending requests to
 *                    keep up subscriptions in tests
 * @return {FirebaseClient} the Firebase client instance
 */
export function initClient(endpointUrl, tenantProvider, keepUpInterval) {
  return initSpineWeb({
    protoIndexFiles: [testProtobuf],
    endpointUrl: endpointUrl,
    firebaseDatabase: firebaseDatabase,
    actorProvider: new ActorProvider(),
    tenantProvider: tenantProvider,
    subscriptionKeepUpInterval: keepUpInterval
  });
}

/**
 * A {@link FirebaseClient client} instance for tests.
 *
 * @type {FirebaseClient}
 */
export const client =
    initClient(TestEnvironment.ENDPOINT, new TenantProvider(TestEnvironment.tenantId()));
