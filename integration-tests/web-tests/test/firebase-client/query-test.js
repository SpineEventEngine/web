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
import {Duration} from '@lib/client/time-utils';
import {ServerError} from '@lib/client/errors';
import {fail} from '../test-helpers';
import TestEnvironment from './given/test-environment';
import {client} from './given/firebase-client';

describe('FirebaseClient', function () {

    // Big timeout due to remote calls during tests.
    const timeoutDuration = new Duration({minutes: 2});
    this.timeout(timeoutDuration.inMs());

    describe('"fetchById"', function () {

        it('returns `null` as a value when fetches entity by ID that is missing', done => {
            const taskId = TestEnvironment.taskId({});

            client.fetchById(TestEnvironment.TYPE.OF_ENTITY.TASK, taskId, data => {
                assert.equal(data, null);

                done();

            }, fail(done));
        });
    });

    describe('"fetchAll"', function () {

        it('retrieves the existing entities of given type one by one', done => {
            const command = TestEnvironment.createTaskCommand({withPrefix: 'spine-web-test-one-by-one'});
            const taskId = command.getId();

            client.sendCommand(command, () => {

                let itemFound = false;

                client.fetchAll({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK}).oneByOne().subscribe({
                    next(data) {
                        // Ordering is not guaranteed by fetch and
                        // the list of entities cannot be cleaned for tests,
                        // thus at least one of entities should match the target one.
                        itemFound = data.getId().getValue() === taskId.getValue() || itemFound;
                    },
                    error: fail(done),
                    complete() {
                        assert.ok(itemFound);
                        done();
                    }
                });

            }, fail(done), fail(done));
        });

        it('retrieves the existing entities of given type at once', done => {
            const command = TestEnvironment.createTaskCommand({withPrefix: 'spine-web-test-at-once'});
            const taskId = command.getId();

            client.sendCommand(command, () => {

                client.fetchAll({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK}).atOnce()
                    .then(data => {
                        const targetObject = data.find(item => item.getId().getValue() === taskId.getValue());
                        assert.ok(targetObject);
                        done();
                    }, fail(done));

            }, fail(done), fail(done));
        });

        it('retrieves an empty list for entity that does not get created at once', done => {
            client.fetchAll({ofType: TestEnvironment.TYPE.OF_ENTITY.PROJECT}).atOnce()
                .then(data => {
                    assert.ok(data.length === 0);
                    done();
                }, fail(done));
        });

        it('retrieves an empty list for entity that does not get created one-by-one', done => {
            client.fetchAll({ofType: TestEnvironment.TYPE.OF_ENTITY.PROJECT}).oneByOne()
                .subscribe({
                    next: fail(done),
                    error: fail(done),
                    complete: () => done()
                });
        });

        it('fails a malformed query', done => {
            const command = TestEnvironment.createTaskCommand({withPrefix: 'spine-web-test-malformed-query'});

            client.sendCommand(command, () => {

                client.fetchAll({ofType: TestEnvironment.TYPE.MALFORMED}).atOnce()
                    .then(fail(done), error => {
                        assert.ok(error instanceof ServerError);
                        assert.equal(error.message, 'Server Error');
                        done();
                    });

            }, fail(done), fail(done));
        });
    });
});
