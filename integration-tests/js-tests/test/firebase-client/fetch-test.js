/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import {ServerError} from '@lib/client/errors';
import {fail} from '../test-helpers';
import TestEnvironment from '../given/test-environment';
import {Task} from '@testProto/spine/web/test/given/task_pb';
import {Project} from '@testProto/spine/web/test/given/project_pb';
import {client} from './given/firebase-client';

describe('FirebaseClient "fetch"', function () {
    let taskIds;

    /**
     * Prepares the environment for `FirebaseClient#fetch()` tests where
     * two tasks are created.
     */
    before(function (done) {
        // Big timeout allows complete environment setup.
        this.timeout(20 * 1000);

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

            client.command(command)
                .onOk(() => reportTaskCreated())
                .onError(fail(done))
                .onImmediateRejection(fail(done))
                .post();
        });

        Promise.all(createTasksPromises)
            .then(() => {
                // Gives time for the model state to be updated
                setTimeout(done, 500);
            });
    });

    it('returns all values of a type', done => {
        client.select(Task)
            .run()
            .then(data => {
                taskIds.forEach(taskId => {
                    const targetObject = data.find(
                        item => item.getId().getValue() === taskId.getValue()
                    );
                    assert.ok(targetObject);
                });
                done();
            })
            .catch((e) => {
                console.error(e);
                fail(done);
            });
    });

    it('returns the correct value by a single ID', done => {
        const id = taskIds[0];
        client.select(Task)
            .byId(id)
            .run()
            .then(data => {
                assert.ok(Array.isArray(data));
                assert.strictEqual(data.length, 1);
                const item = data[0];
                assert.ok(item.getId().getValue() === id.getValue());
                done();
            })
            .catch((e) => {
                console.error(e);
                fail(done);
            });
    });

    it('ignores `byId` parameter when an empty list is specified', done => {
        client.select(Task)
            .byId([])
            .run()
            .then(data => {
                assert.ok(Array.isArray(data));
                assert.ok(data.length >= taskIds.length);
                done();
            })
            .catch((e) => {
                console.error(e);
                fail(done);
            });
    });

    it('ignores `byId` parameter when a `null` value is specified', done => {
        client.select(Task)
            .byId(null)
            .run()
            .then(data => {
                assert.ok(Array.isArray(data));
                assert.ok(data.length >= taskIds.length);
                done();
            })
            .catch((e) => {
                console.error(e);
                fail(done);
            });
    });

    it('returns empty list when fetches entity by a single ID that is missing', done => {
        const taskId = TestEnvironment.taskId({});

        client.select(Task)
            .byId(taskId)
            .run()
            .then(data => {
                assert.ok(Array.isArray(data));
                assert.ok(data.length === 0);
                done();
            })
            .catch((e) => {
                console.error(e);
                fail(done);
            });
    });

    it('returns correct values by multiple IDs', done => {
        client.select(Task)
            .byId(taskIds)
            .run()
            .then(data => {
                assert.ok(Array.isArray(data));
                assert.strictEqual(data.length, taskIds.length);
                taskIds.forEach(taskId => {
                    const targetObject = data.find(item => item.getId().getValue() === taskId.getValue());
                    assert.ok(targetObject);
                });

                done();
            })
            .catch((e) => {
                console.error(e);
                fail(done);
            });
    });

    it('retrieves an empty list for an entity type that does not get instantiated', done => {
        client.select(Project)
            .run()
            .then(data => {
                assert.ok(data.length === 0);
                done();
            })
            .catch((e) => {
                console.error(e);
                fail(done);
            });
    });

    it('fetches entities using a manually created `Query`', done => {
        const query = client.newQuery()
            .select(Task)
            .byIds(taskIds)
            .build();
        client.read(query)
            .then(data => {
                assert.ok(Array.isArray(data));
                assert.strictEqual(data.length, taskIds.length);
                taskIds.forEach(taskId => {
                    const targetObject = data.find(item => item.getId().getValue() === taskId.getValue());
                    assert.ok(targetObject);
                });
                done();
            })
            .catch((e) => {
                console.error(e);
                fail(done);
            });
    });

    it('fails a malformed query', done => {
        const command =
            TestEnvironment.createTaskCommand({withPrefix: 'spine-web-test-malformed-query'});

        const Unknown = class {
            static typeUrl() {
                return 'spine.web/fails.malformed.type'
            }
        };
        const selectAndCheckFailed = () => client.select(Unknown)
            .run()
            .then(fail(done), error => {
                assert.ok(error instanceof ServerError);
                assert.strictEqual(error.message, 'Server Error');
                done();
            });
        client.command(command)
            .onOk(selectAndCheckFailed)
            .onError(fail(done))
            .onImmediateRejection(fail(done))
            .post();
    });
});
