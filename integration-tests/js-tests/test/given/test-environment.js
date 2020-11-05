/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import {v4 as uuidv4} from 'uuid';
import {TenantIds} from '@lib/client/tenant';
import {CreateTask, RenameTask} from '@testProto/spine/web/test/given/commands_pb';
import {TaskId} from '@testProto/spine/web/test/given/task_pb';
import {AddUserInfo} from '@testProto/spine/web/test/given/user_commands_pb';
import {UserId} from '@testProto/spine/core/user_id_pb';

/**
 * @typedef {Object} User a type representing a user with his tasks for test purposes
 *
 * @property {UserId} id
 * @property {String} name
 * @property {TaskId[]} tasks
 */

/**
 * The class for preparation of a test environment using a sample application
 * defined in `test-app` module. Allows to create and update tasks.
 */
export default class TestEnvironment {

  constructor() {
    throw new Error('A utility `TestEnvironment` class cannot be instantiated.');
  }

  /**
   * @param {{
   *     withId?: String,
   *     withPrefix?: String,
   *     named?: String,
   *     describedAs: String,
   *     assignedTo: UserId,
   *     rejectCommand: Boolean
   * }}
   *
   * @return {CreateTask}
   */
  static createTaskCommand({
                             withId: id,
                             withPrefix: idPrefix,
                             named: name,
                             describedAs: description,
                             assignedTo: userId,
                             rejectCommand: reject
                           }) {
    const taskId = this.taskId({value: id, withPrefix: idPrefix});

    name = typeof name === 'undefined' ? this.DEFAULT_TASK_NAME : name;
    description = typeof description === 'undefined'
        ? this.DEFAULT_TASK_DESCRIPTION
        : description;

    const command = new CreateTask();
    command.setId(taskId);
    command.setName(name);
    command.setDescription(description);

    if (!!userId) {
      command.setAssignee(userId);
    }
    if (reject) {
      command.setReject(true);
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
   * @param {!string} fullName
   * @return {AddUserInfo}
   */
  static addUserInfoCommand(fullName) {
    const userId = TestEnvironment.userId();
    const cmd = new AddUserInfo();
    cmd.setId(userId);
    cmd.setFullName(fullName);
    return cmd;
  }

  /**
   * @param value
   * @param withPrefix
   *
   * @return {TaskId}
   */
  static taskId({value, withPrefix: prefix}) {
    if (typeof value === 'undefined') {
      value = uuidv4();
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
   * @return {UserId}
   */
  static userId(withPrefix) {
    const id = new UserId();
    id.setValue(`${withPrefix ? withPrefix : 'ANONYMOUS'}-${uuidv4()}`);
    return id;
  }

  /**
   * The tenant ID to use in multitenant tests.
   *
   * Please, make sure that the root with the same name is accessible for reading in the test
   * Firebase database.
   */
  static tenantId() {
    return TenantIds.plainString('maia');
  }
}

TestEnvironment.DEFAULT_TASK_NAME = 'Get to Mount Doom';
TestEnvironment.DEFAULT_TASK_DESCRIPTION = 'There seems to be a bug with the rings that needs to be fixed';

TestEnvironment.ENDPOINT = 'http://localhost:8080';
