/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import * as testProtobuf from '@testProto/index';
import {Client} from '@lib/client/client';
import * as spineWeb from '@lib/index';

/**
 * A helper functions to fail fast async tests.
 *
 * Can be used in callback-based async tests to fail them before waiting
 * of the full test timeout.
 *
 * @example
 * // To fail the test when checking Promise that should be rejected
 * promiseThatShouldBeRejected
 *   .then(fail(done, 'Expecting promise to be rejected'))
 *   .catch(error => {
 *     checkError(error);
 *     done();
 *    });
 * @example
 * // To fail the test when checking Promise that should be resolved
 * promiseThatShouldBeResolved
 *   .then(value => {
 *     checkValue(value);
 *     done();
 *    })
 *   .catch(fail(done))
 *
 * @param {function(*=)} done the callback that should be called when your test is complete
 * @param {string=} message the test failure message
 * @return {function(*=)} a function to fail the test. Accepts the first parameter as a cause for test failure.
 */
export function fail(done, message = '') {
  return cause => {
    if (message) {
     done(new Error(`Test failed. Cause: ${message}`));
    } else {
     done(new Error(`Test failed. Cause: ${cause ? JSON.stringify(cause) : 'not identified'}`));
    }
  };
}

export class MockClient extends Client {
    // There is no need to implement `Client` for tests
    // which don't use its API
}

export function registerProtobufTypes() {
    spineWeb.init({
       protoIndexFiles: [testProtobuf],
       implementation: new MockClient()
    });
}
