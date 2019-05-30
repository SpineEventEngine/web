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
import {fail} from '../test-helpers';
import TestEnvironment from './given/test-environment';
import {client} from './given/firebase-client';

describe('FirebaseClient subscription', function () {

    // Big timeout allows to receive model state changes during tests.
    this.timeout(120 * 1000);

    it('retrieves new entities', done => {
        const names = ['Task #1', 'Task #2', 'Task #3'];
        const newTasksCount = names.length;
        let receivedCount = 0;

        const commands = names.map(name => TestEnvironment.createTaskCommand({
            withPrefix: 'spine-web-test-subscribe',
            named: name
        }));

        const taskIds = commands.map(command => command.getId().getValue());
        commands.forEach(command => {
            client.sendCommand(command, TestEnvironment.noop, fail(done), fail(done));
        });

        client.subscribeToEntities({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK})
            .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
                itemAdded.subscribe({
                    next: task => {
                        const id = task.getId().getValue();
                        console.log(`Retrieved task '${id}'`);
                        if (taskIds.includes(id)) {
                            receivedCount++;
                            if (receivedCount === newTasksCount) {
                                unsubscribe();
                                done();
                            }
                        }
                    }
                });
                itemRemoved.subscribe({
                    next: fail(done, 'Unexpected entity remove during entity create subscription test.')
                });
                itemChanged.subscribe({
                    next: fail(done, 'Unexpected entity change during entity create subscription test.')
                });
            })
            .catch(fail(done));
    });

    it('retrieves updates when subscribed by type', done => {
        const INITIAL_TASK_NAME = "Task to test entity updates";
        const UPDATED_TASK_NAME = "RENAMED Task to test entity updates";
        let taskId;
        let reportItemAdded;
        const itemAddedPromise = new Promise(resolve => reportItemAdded = resolve);

        client.subscribeToEntities({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK})
            .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
                itemAdded.subscribe({
                    next: item => {
                        const id = item.getId().getValue();
                        console.log(`Retrieved new task '${id}'.`);
                        if (taskId === id) {
                            assert.ok(
                                INITIAL_TASK_NAME === item.getName(),
                                `Task is named "${item.getName()}", expected "${INITIAL_TASK_NAME}"`
                            );
                            reportItemAdded();
                        }
                    }
                });
                itemRemoved.subscribe({
                    next: fail(done, 'Task was removed in a test of entity changes subscription.')
                });
                itemChanged.subscribe({
                    next: item => {
                        const id = item.getId().getValue();
                        if (taskId === id) {
                            assert.ok(
                                item.getName() === UPDATED_TASK_NAME,
                                `Task is named "${item.getName()}", expected "${UPDATED_TASK_NAME}"`
                            );
                            console.log(`Got task changes for ${id}.`);
                            unsubscribe();
                            done();
                        }
                    }
                });
            })
            .catch(fail(done));

        // Create task.
        const createCommand = TestEnvironment.createTaskCommand({
            withPrefix: 'spine-web-test-subscribe',
            named: INITIAL_TASK_NAME
        });
        taskId = createCommand.getId().getValue();

        client.sendCommand(
            createCommand,
            () => console.log(`Task '${createCommand.getId().getValue()}' created.`),
            fail(done, 'Unexpected error while creating a task.'),
            fail(done, 'Unexpected rejection while creating a task.')
        );

        // Rename created task after the `itemAdded` subscription was received.
        itemAddedPromise.then(() => {
            const renameCommand = TestEnvironment.renameTaskCommand({
                withId: taskId,
                to: UPDATED_TASK_NAME
            });
            client.sendCommand(
                renameCommand,
                () => console.log(`Task '${taskId}' renamed.`),
                fail(done, 'Unexpected error while renaming a task.'),
                fail(done, 'Unexpected rejection while renaming a task.')
            );
        });
    });

    it('retrieves updates by id', done => {
        const expectedChangesCount = 2;
        const INITIAL_TASK_NAME = 'Initial task name';
        const UPDATED_NAMES = ['Renamed once', 'Renamed twice'];

        // Create tasks.
        const createCommand = TestEnvironment.createTaskCommand({
            withPrefix: 'spine-web-test-subscribe',
            named: INITIAL_TASK_NAME
        });
        const taskId = createCommand.getId();
        const taskIdValue = createCommand.getId().getValue();

        let reportItemAdded;
        const itemAddedPromise = new Promise(resolve => reportItemAdded = resolve);
        client.sendCommand(
            createCommand,
            () => console.log(`Task '${taskIdValue}' created.`),
            fail(done, 'Unexpected error while creating a task.'),
            fail(done, 'Unexpected rejection while creating a task.')
        );

        let reportItemRenamedAtFirst;
        const itemRenamedAtFirstPromise = new Promise(resolve =>
            reportItemRenamedAtFirst = resolve);
        let changesCount = 0;
        client.subscribeToEntities({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK, byId: taskId})
            .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
                itemAdded.subscribe({
                    next: item => {
                        const id = item.getId().getValue();
                        console.log(`Retrieved new task '${id}'.`);
                        if (taskIdValue === id) {
                            assert.equal(
                                item.getName(), INITIAL_TASK_NAME,
                                `Task is named "${item.getName()}", expected "${INITIAL_TASK_NAME}"`
                            );
                        }
                        reportItemAdded();
                    }
                });
                itemRemoved.subscribe({
                    next: fail(done, 'Task was removed in a test of entity changes subscription.')
                });
                itemChanged.subscribe({
                    next: item => {
                        const id = item.getId().getValue();
                        if (taskIdValue === id) {
                            console.log(`Got task changes for ${id}.`);
                            assert.equal(item.getName(), UPDATED_NAMES[changesCount]);
                            changesCount++;
                            if (changesCount === expectedChangesCount) {
                                unsubscribe();
                                done();
                            } else {
                                reportItemRenamedAtFirst();
                            }
                        }
                    }
                });
            })
            .catch(fail(done));

        itemAddedPromise.then(() => {
            const renameCommand = TestEnvironment.renameTaskCommand({
                withId: taskIdValue,
                to: UPDATED_NAMES[0]
            });
            client.sendCommand(
                renameCommand,
                () => console.log(`Task '${taskIdValue}' renamed for the first time.`),
                fail(done, 'Unexpected error while renaming a task.'),
                fail(done, 'Unexpected rejection while renaming a task.')
            );
        });
        itemRenamedAtFirstPromise.then(() => {
            const renameCommand = TestEnvironment.renameTaskCommand({
                withId: taskIdValue,
                to: UPDATED_NAMES[1]
            });
            client.sendCommand(
                renameCommand,
                () => console.log(`Task '${taskIdValue}' renamed for the second time.`),
                fail(done, 'Unexpected error while renaming a task.'),
                fail(done, 'Unexpected rejection while renaming a task.')
            );
        });
    });

    it('fails for a malformed entity type', done => {
        client.subscribeToEntities({ofType: TestEnvironment.TYPE.MALFORMED})
            .then(() => {
                done(new Error('A malformed subscription should not yield results.'));
            })
            .catch(error => {
                assert.ok(true);
                done();
            });
    });
});
