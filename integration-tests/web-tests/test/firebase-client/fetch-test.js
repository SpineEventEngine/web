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
import {ServerError} from '@lib/client/errors';
import {fail} from '../test-helpers';
import TestEnvironment from './given/test-environment';
import {client} from './given/firebase-client';

describe('FirebaseClient', function () {

    describe('"fetchById"', function () {

        it('returns `null` as a value when fetches entity by ID that is missing', done => {
            const taskId = TestEnvironment.taskId({});

            client.fetchById({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK, id: taskId})
                .then(data => {
                    assert.equal(data, null);
                    done();
                }, fail(done));
        });
    });

    describe('"fetchAll"', function () {

        it('retrieves the existing entities of given type', done => {
            const command = TestEnvironment.createTaskCommand({withPrefix: 'spine-web-test-fetch-all'});
            const taskId = command.getId();

            client.sendCommand(command, () => {

                client.fetchAll({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK})
                    .then(data => {
                        const targetObject = data.find(item => item.getId().getValue() === taskId.getValue());
                        assert.ok(targetObject);
                        done();
                    }, fail(done));

            }, fail(done), fail(done));
        });

        it('retrieves an empty list for entity that does not get created', done => {
            client.fetchAll({ofType: TestEnvironment.TYPE.OF_ENTITY.PROJECT})
                .then(data => {
                    assert.ok(data.length === 0);
                    done();
                }, fail(done));
        });

        it('fails a malformed query', done => {
            const command = TestEnvironment.createTaskCommand({withPrefix: 'spine-web-test-malformed-query'});

            client.sendCommand(command, () => {

                client.fetchAll({ofType: TestEnvironment.TYPE.MALFORMED})
                    .then(fail(done), error => {
                        assert.ok(error instanceof ServerError);
                        assert.equal(error.message, 'Server Error');
                        done();
                    });

            }, fail(done), fail(done));
        });
    });
});
