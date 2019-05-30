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

import uuid from 'uuid';
import {CreateTask, RenameTask} from '@testProto/spine/web/test/given/commands_pb';
import {Task, TaskId} from '@testProto/spine/web/test/given/task_pb';
import {Project} from '@testProto/spine/web/test/given/project_pb';
import {UserTasks} from '@testProto/spine/web/test/given/user_tasks_pb';
import {UserId} from '@testProto/spine/core/user_id_pb';
import {Type} from '@lib/client/typed-message';

export default class TestEnvironment {

    constructor() {
        throw new Error('A utility TestEnvironment class cannot be instantiated.');
    }

    /**
     * @param {{
     *     withId?: String,
     *     withPrefix?: String,
     *     named?: String,
     *     describedAs: String,
     *     assignedTo: UserId
     * }}
     *
     * @return {CreateTask}
     */
    static createTaskCommand({
                                 withId: id,
                                 withPrefix: idPrefix,
                                 named: name,
                                 describedAs: description,
                                 assignedTo: userId
                             }) {
        const taskId = this.taskId({value: id, withPrefix: idPrefix});

        name = typeof name === 'undefined' ? this.DEFAULT_TASK_NAME : name;
        description = typeof description === 'undefined' ? this.DEFAULT_TASK_DESCRIPTION : description;

        const command = new CreateTask();
        command.setId(taskId);
        command.setName(name);
        command.setDescription(description);

        if (!!userId) {
            command.setAssignee(userId);
        }

        return command;
    }

    /**
     * @param {{
     *     withId!: String,
     *     to!: String
     * }}
     *
     * @return {RenameTask}
     */
    static renameTaskCommand({withId: id, to: newName}) {
        const taskId = this.taskId({value: id});

        const command = new RenameTask();
        command.setId(taskId);
        command.setName(newName);

        return command;

    }

    /**
     * @param value
     * @param withPrefix
     *
     * @return {TaskId}
     */
    static taskId({value, withPrefix: prefix}) {
        if (typeof value === 'undefined') {
            value = uuid.v4();
        }
        if (typeof prefix !== 'undefined') {
            value = `${prefix}-${value}`;
        }
        const taskId = new TaskId();
        taskId.setValue(value);
        return taskId;
    }

    /**
     * @param {?String} withPrefix
     */
    static userId(withPrefix) {
        const id = new UserId();
        id.setValue(`${withPrefix ? withPrefix : 'ANONYMOUS'}-${uuid.v4()}`);
        return id;
    }

    /**
     * A function that does nothing.
     */
    static noop() {
        // Do nothing.
    }
}

TestEnvironment.DEFAULT_TASK_NAME = 'Get to Mount Doom';
TestEnvironment.DEFAULT_TASK_DESCRIPTION = 'There seems to be a bug with the rings that needs to be fixed';

TestEnvironment.TYPE = {
    OF_ENTITY: {
        TASK: Type.forClass(Task),
        PROJECT: Type.forClass(Project),
        USER_TASKS: Type.forClass(UserTasks)
    },
    MALFORMED: Type.of(Object, 'types.spine.io/malformed'),
};
