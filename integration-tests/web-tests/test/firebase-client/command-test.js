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
import TestEnvironment from './given/test-environment';
import {
    CommandValidationError,
    CommandHandlingError,
    ConnectionError
} from '@lib/index';
import {fail} from '../test-helpers';
import {client, initClient} from './given/firebase-client';

describe('FirebaseClient command sending', function () {

    // Big timeout allows to receive model state changes during tests.
    this.timeout(5000);

    it('completes with success', done => {

        const command = TestEnvironment.createTaskCommand({
            withPrefix: 'spine-web-test-send-command',
            named: 'Implement Spine Web JS client tests',
            describedAs: 'Spine Web need integration tests'
        });

        const taskId = command.getId();

        client.sendCommand(command, () => {

            client.fetchById(TestEnvironment.TYPE.OF_ENTITY.TASK, taskId, data => {
                assert.equal(data.getId().getValue(), taskId);
                assert.equal(data.getName(), command.getName());
                assert.equal(data.getDescription(), command.getDescription());

                done();

            }, fail(done));

        }, fail(done), fail(done));
    });

    it('fails when wrong server endpoint specified', done => {
        const fakeBaseUrl = 'https://malformed-server-endpoint.com';
        const malformedBackendClient = initClient(fakeBaseUrl);
        const command = TestEnvironment.createTaskCommand({
            withPrefix: 'spine-web-test-send-command',
            named: 'Implement Spine Web JS client tests',
            describedAs: 'Spine Web need integration tests'
        });

        malformedBackendClient.sendCommand(
            command,
            fail(done, 'A command was acknowledged when it was expected to fail.'),
            error => {
                try {
                    assert.ok(error instanceof CommandHandlingError);
                    assert.ok(error.message.startsWith(`request to ${fakeBaseUrl}/command failed`));
                    const connectionError = error.getCause();
                    assert.ok(connectionError instanceof ConnectionError);
                    done();
                } catch (e) {
                    fail(done, e.message)
                }
            },
            fail(done, 'A command was rejected when an error was expected.'));
    });

    it('fails with `CommandValidationError` for invalid command message', done => {
        const command = TestEnvironment.createTaskCommand({withId: null});

        client.sendCommand(
            command,
            fail(done, 'A command was acknowledged when it was expected to fail.'),
            error => {
                try {
                    assert.ok(error instanceof CommandValidationError);
                    assert.ok(error.validationError());
                    // assert.ok(error.assuresCommandNeglected());

                    const cause = error.getCause();
                    assert.ok(cause);
                    assert.equal(cause.getCode(), 2);
                    assert.equal(cause.getType(), 'spine.core.CommandValidationError');
                    done();
                } catch (e) {
                    fail(done, e.message)
                }
            },
            fail(done, 'A command was rejected when an error was expected.'));
    });
});
