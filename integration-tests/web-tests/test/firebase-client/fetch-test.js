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
import {Task} from '@testProto/spine/web/test/given/task_pb';
import {Project} from '@testProto/spine/web/test/given/project_pb';
import {client} from './given/firebase-client';

describe('FirebaseClient "fetch"', function () {
    let taskIds;

    /**
     * Prepares the environment for `FirebaseClient#fetch()` tests where
     * two tasks are created.
     */
    before((done) => {
        const createTaskCommands = [
            TestEnvironment.createTaskCommand({withPrefix: 'spine-web-fetch-test-task-1'}),
            TestEnvironment.createTaskCommand({withPrefix: 'spine-web-fetch-test-task-2'}),
        ];

        taskIds = createTaskCommands.map(command => command.getId());

        const createTasksPromises = [];
        createTaskCommands.forEach(command => {
            let reportTaskCreated;
            const promise = new Promise(resolve => reportTaskCreated = resolve);
            createTasksPromises.push(promise);

            client.sendCommand(command, () => reportTaskCreated(), fail(done), fail(done))
        });

        Promise.all(createTasksPromises)
            .then(() => {
                // Gives time for the model state to be updated
                setTimeout(() => done(), 100);
            })
            .catch(fail(done));
    });

    it('returns correct value by single ID', done => {
        const id = taskIds[0];
        client.fetch({entity: Task, byIds: id})
            .then(item => {
                assert.ok(!Array.isArray(item));
                assert.ok(item.getId().getValue() === id.getValue());
                done();
            }, fail(done));
    });

    it('ignores `byIds` parameter when empty list specified', done => {
        client.fetch({entity: Task, byIds: []})
            .then(items => {
                assert.ok(Array.isArray(items));
                assert.ok(items.length >= taskIds.length);
                done();
            }, fail(done));
    });

    it('ignores `byIds` parameter when `null` value specified', done => {
        client.fetch({entity: Task, byIds: null})
            .then(items => {
                assert.ok(Array.isArray(items));
                assert.ok(items.length >= taskIds.length);
                done();
            }, fail(done));
    });

    it('ignores `byIds` parameter when empty list specified', done => {
        client.fetch({entity: Task, byIds: []})
            .then(items => {
                assert.ok(Array.isArray(items));
                done();
            }, fail(done));
    });


    it('returns `null` as a value when fetches entity by single ID that is missing', done => {
        const taskId = TestEnvironment.taskId({});

        client.fetch({entity: Task, byIds: taskId})
            .then(item => {
                assert.ok(!Array.isArray(item));
                assert.equal(item, null);
                done();
            }, fail(done));
    });

    it('returns correct values by IDs', done => {
        client.fetch({entity: Task, byIds: taskIds})
            .then(data => {
                assert.ok(Array.isArray(data));
                assert.equal(data.length, taskIds.length);
                taskIds.forEach(taskId => {
                    const targetObject = data.find(item => item.getId().getValue() === taskId.getValue());
                    assert.ok(targetObject);
                });

                done();
            }, fail(done));
    });

    it('retrieves the existing entities of given type when no IDs specified', done => {
        client.fetch({entity: Task})
            .then(data => {
                taskIds.forEach(taskId => {
                    const targetObject = data.find(item => item.getId().getValue() === taskId.getValue());
                    assert.ok(targetObject);
                });

                done();
            }, fail(done));
    });

    it('retrieves an empty list for entity that does not get created', done => {
        client.fetch({entity: Project})
            .then(data => {
                assert.ok(data.length === 0);
                done();
            }, fail(done));
    });

    it('fails a malformed query', done => {
        const command = TestEnvironment.createTaskCommand({withPrefix: 'spine-web-test-malformed-query'});

        const Unknown = class {
            static typeUrl() {
                return 'spine.web/fails.malformed.type'
            }
        };

        client.sendCommand(command, () => {

            client.fetch({entity: Unknown})
                .then(fail(done), error => {
                    assert.ok(error instanceof ServerError);
                    assert.equal(error.message, 'Server Error');
                    done();
                });

        }, fail(done), fail(done));
    });
});
