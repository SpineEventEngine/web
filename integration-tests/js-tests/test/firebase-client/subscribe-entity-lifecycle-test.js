/*
 * Copyright 2023, TeamDev. All rights reserved.
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
import {fail} from '../test-helpers';
import TestEnvironment from '../given/test-environment';
import {Task} from '@testProto/spine/web/test/given/task_pb';
import {client} from './given/firebase-client';

describe('Subscription made with FirebaseClient should', function () {

    // Big timeout allows to receive model state changes during tests.
    this.timeout(120 * 1000);

    it('reflects updates of a task that gets created -> renamed -> deleted', (done) => {
        const INITIAL_TASK_NAME = 'Initial task name';
        const UPDATED_NAME = 'Updated task name';

        const createCommand = TestEnvironment.createTaskCommand({
            withPrefix: 'spine-web-test-entity-lifecycle-subscription',
            named: INITIAL_TASK_NAME
        });
        const sendCreateTask = () => client.command(createCommand)
            .onError(fail(done, 'Unexpected error while creating a task.'))
            .onImmediateRejection(fail(done, 'Unexpected rejection while creating a task.'))
            .post();

        const taskId = createCommand.getId();
        const taskIdValue = taskId.getValue();

        const renameCommand = TestEnvironment.renameTaskCommand({
            withId: taskIdValue,
            to: UPDATED_NAME
        });
        const sendRenameTask = () => client.command(renameCommand)
            .onError(fail(done, 'Unexpected error while renaming a task.'))
            .onImmediateRejection(fail(done, 'Unexpected rejection while renaming a task.'))
            .post();

        const deleteCommand = TestEnvironment.deleteTaskCommand(taskId);
        const sendDeleteTask = () => client.command(deleteCommand)
            .onError(fail(done, 'Unexpected error while deleting a task.'))
            .onImmediateRejection(fail(done, 'Unexpected rejection while deleting a task.'))
            .post();

        let reportTaskCreated, reportTaskRenamed, reportTaskDeleted;
        const taskCreated = new Promise(resolve => reportTaskCreated = resolve);
        const taskRenamed = new Promise(resolve => reportTaskRenamed = resolve);
        const taskDeleted = new Promise(resolve => reportTaskDeleted = resolve);

        client.subscribeTo(Task)
            .byId(taskId)
            .post()
            .then(async ({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
                itemAdded.subscribe(nextItem => {
                    const actualIdValue = nextItem.getId().getValue();
                    const actualTaskName = nextItem.getName();
                    assert.strictEqual(actualIdValue, taskIdValue,
                        `New task has ID "${actualIdValue}", expected "${taskIdValue}".`
                    );
                    assert.strictEqual(actualTaskName, INITIAL_TASK_NAME,
                        `Task is named "${actualTaskName}", expected "${INITIAL_TASK_NAME}".`
                    );

                    reportTaskCreated();
                });

                itemChanged.subscribe(nextItem => {
                    const actualIdValue = nextItem.getId().getValue();
                    const actualTaskName = nextItem.getName();
                    assert.strictEqual(actualIdValue, taskIdValue,
                        `Updated task has ID "${actualIdValue}", expected "${taskIdValue}".`
                    );
                    assert.strictEqual(actualTaskName, UPDATED_NAME,
                        `Renamed task is named "${actualTaskName}", expected "${UPDATED_NAME}".`
                    );

                    reportTaskRenamed();
                });

                itemRemoved.subscribe(nextItem => {
                    const actualIdValue = nextItem.getId().getValue();
                    assert.strictEqual(actualIdValue, taskIdValue,
                        `Deleted task has ID "${actualIdValue}", expected "${taskIdValue}".`
                    );

                    reportTaskDeleted();
                });

                sendCreateTask();
                await taskCreated;

                sendRenameTask();
                await taskRenamed;

                sendDeleteTask();
                await taskDeleted;
            })
            .then(done)
            .catch(fail(done));
    });
});