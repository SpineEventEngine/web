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
import {fail} from '../test-helpers';
import TestEnvironment from './given/test-environment';
import {client} from './given/firebase-client';

describe('FirebaseClient subscription', function () {

    // Big timeout due to remote calls during tests.
    const timeoutDuration = new Duration({minutes: 2});
    this.timeout(timeoutDuration.inMs());

    it('retrieves new entities', done => {
        const names = ['Task #1', 'Task #2', 'Task #3'];
        const tasksToBeCreated = names.length;
        let taskIds;
        let count = 0;
        client.subscribeToEntities({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK})
            .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
                itemAdded.subscribe({
                    next: task => {
                        const id = task.getId().getValue();
                        console.log(`Retrieved task '${id}'`);
                        if (taskIds.includes(id)) {
                            count++;
                            if (count === tasksToBeCreated) {
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

        const commands = TestEnvironment.createTaskCommands({
            withPrefix: 'spine-web-test-subscribe',
            named: names
        });
        taskIds = commands.map(command => command.getId().getValue());
        commands.forEach(command => {
            client.sendCommand(command, TestEnvironment.noop, fail(done), fail(done));
        });
    });

    it('retrieves updates of entities', done => {
        const TASKS_TO_BE_CHANGED = 3;
        let taskIds;
        let countChanged = 0;
        const initialTaskNames = ['Created task #1', 'Created task #2', 'Created task #3'];

        client.subscribeToEntities({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK})
            .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
                itemAdded.subscribe({
                    next: item => {
                        const id = item.getId().getValue();
                        console.log(`Retrieved new task '${id}'.`);
                        if (taskIds.includes(id)) {
                            assert.ok(
                                initialTaskNames.includes(item.getName()),
                                `Task is named "${item.getName()}", expected one of [${initialTaskNames}]`
                            );
                        }
                    }
                });
                itemRemoved.subscribe({
                    next: fail(done, 'Task was removed in a test of entity changes subscription.')
                });
                itemChanged.subscribe({
                    next: item => {
                        const id = item.getId().getValue();
                        if (taskIds.includes(id)) {
                            console.log(`Got task changes for ${id}.`);
                            countChanged++;
                            if (countChanged === TASKS_TO_BE_CHANGED) {
                                unsubscribe();
                                done();
                            }
                        } else {
                            done(new Error('Unexpected entity changes during subscription to entity changes test'));
                        }
                    }
                });
            })
            .catch(fail(done));

        // Create tasks.
        const createCommands = TestEnvironment.createTaskCommands({
            count: TASKS_TO_BE_CHANGED,
            withPrefix: 'spine-web-test-subscribe',
            named: initialTaskNames
        });
        taskIds = createCommands.map(command => command.getId().getValue());
        const createPromises = [];
        createCommands.forEach(command => {
            const promise = new Promise(resolve => {
                client.sendCommand(
                    command,
                    () => {
                        console.log(`Task '${command.getId().getValue()}' created.`);
                        resolve();
                    },
                    fail(done, 'Unexpected error while creating a task.'),
                    fail(done, 'Unexpected rejection while creating a task.')
                );
            });
            createPromises.push(promise);
        });

        // Rename created tasks.
        Promise.all(createPromises).then(() => {
            // Rename tasks in a timeout after they are created to
            // allow for added subscriptions to be updated first.
            const renameTimeout = new Duration({seconds: 30});
            setTimeout(() => {
                taskIds.forEach(taskId => {
                    const renameCommand = TestEnvironment.renameTaskCommand({
                        withId: taskId,
                        to: `Renamed '${taskId}'`
                    });
                    client.sendCommand(
                        renameCommand,
                        () => console.log(`Task '${taskId}' renamed.`),
                        fail(done, 'Unexpected error while renaming a task.'),
                        fail(done, 'Unexpected rejection while renaming a task.')
                    );
                });
            }, renameTimeout.inMs());
        });
    });

    it('retrieves updates by id', done => {
        const expectedChangesCount = 2;
        const initialTaskName = 'Initial task name';
        const expectedRenames = ['Renamed once', 'Renamed twice'];

        // Create tasks.
        const createCommand = TestEnvironment.createTaskCommand({
            withPrefix: 'spine-web-test-subscribe',
            named: initialTaskName
        });
        const taskId = createCommand.getId();
        const taskIdValue = createCommand.getId().getValue();

        const promise = new Promise(resolve => {
            client.sendCommand(
                createCommand,
                () => {
                    console.log(`Task '${taskIdValue}' created.`);
                    resolve();
                },
                fail(done, 'Unexpected error while creating a task.'),
                fail(done, 'Unexpected rejection while creating a task.')
            );
        });

        let changesCount = 0;
        client.subscribeToEntities({ofType: TestEnvironment.TYPE.OF_ENTITY.TASK, byId: taskId})
            .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
                itemAdded.subscribe({
                    next: item => {
                        const id = item.getId().getValue();
                        console.log(`Retrieved new task '${id}'.`);
                        if (taskIdValue === id) {
                            assert.equal(
                                item.getName(), initialTaskName,
                                `Task is named "${item.getName()}", expected "${initialTaskName}"`
                            );
                        } else {
                            done(new Error(`Only changes for task with ID ${taskIdValue} should be received.`))
                        }
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
                            assert.equal(item.getName(), expectedRenames[changesCount]);
                            changesCount++;
                            if (changesCount === expectedChangesCount) {
                                unsubscribe();
                                done();
                            }
                        } else {
                            done(new Error('Unexpected entity changes during subscription to entity changes test'));
                        }
                    }
                });
            })
            .catch(fail(done));

        // Rename created task.
        const renameTimeout = new Duration({seconds: 20});
        promise.then(() => {
            // Tasks are renamed with a timeout after to allow for changes to show up in subscriptions.
            return new Promise(resolve => {
                setTimeout(() => {
                    const renameCommand = TestEnvironment.renameTaskCommand({
                        withId: taskIdValue,
                        to: 'Renamed once'
                    });
                    client.sendCommand(
                        renameCommand,
                        () => {
                            resolve();
                            console.log(`Task '${taskIdValue}' renamed for the first time.`)
                        },
                        fail(done, 'Unexpected error while renaming a task.'),
                        fail(done, 'Unexpected rejection while renaming a task.')
                    );
                }, renameTimeout.inMs());
            });
        }).then(() => {
            setTimeout(() => {
                const renameCommand = TestEnvironment.renameTaskCommand({
                    withId: taskIdValue,
                    to: 'Renamed twice'
                });
                client.sendCommand(
                    renameCommand,
                    () => console.log(`Task '${taskIdValue}' renamed for the second time.`),
                    fail(done, 'Unexpected error while renaming a task.'),
                    fail(done, 'Unexpected rejection while renaming a task.')
                );
            }, renameTimeout.inMs());
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
