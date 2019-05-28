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

import {firebaseDatabase} from "./firebase-database";
import * as testProtobuf from '@testProto/index';
import * as spineWeb from '@lib/index';
import {ActorProvider} from '@lib/client/actor-request-factory';

/**
 * Initializes the {@link FirebaseClient client} that interacts with Gretty-based
 * local backend server and the emulated Firebase application.
 * See `integration-tests/README.MD` for details.
 *
 * @param endpointUrl the URL of a backend to interact with; has the default value
 *                    of a local backend server;
 * @return {FirebaseClient} the Firebase client instance
 */
export function initClient(endpointUrl = 'http://localhost:8080') {
    return spineWeb.init({
        protoIndexFiles: [testProtobuf],
        endpointUrl: endpointUrl,
        firebaseDatabase: firebaseDatabase,
        actorProvider: new ActorProvider()
    });
}

/**
 * A {@link FirebaseClient client} instance for tests.
 *
 * @type {FirebaseClient}
 */
export const client = initClient();
