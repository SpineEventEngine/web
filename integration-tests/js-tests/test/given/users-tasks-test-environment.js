/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import {UserId} from '@testProto/spine/core/user_id_pb';
import {ReassignTask} from '@testProto/spine/web/test/given/commands_pb';
import TestEnvironment from './test-environment';

/**
 * The class for preparation of a test environment using a sample application
 * defined in `test-app` module. Allows to create predefined amount of tasks
 * and assign them to sample users.
 */
export class UserTasksTestEnvironment extends TestEnvironment {

  /**
   * Creates requested amount of tasks assigned to the given user.
   *
   * @param {User} user a user to create tasks for
   * @param {number} taskCount an amount of tasks to be created and assigned
   * @param {Client} client a Spine client to send commands
   * @return {Promise} a promise to be resolved when all `CreateTask` commands acknowledged;
   *                   rejected if an error occurs;
   */
  static createTaskFor(user, taskCount, client) {
    const createTaskPromises = [];
    for (let i = 0; i < taskCount; i++) {
      const command = super.createTaskCommand({
        named: `task#${i + 1}-for-${user.name}`,
        assignedTo: user.id
      });
      const taskId = command.getId();

      let createTaskAcknowledged;
      let createTaskFailed;

      const promise = new Promise((resolve, reject) => {
        createTaskAcknowledged = resolve;
        createTaskFailed = reject;
      });
      createTaskPromises.push(promise);
      client.command(command)
          .onOk(() => {
            user.tasks.push(taskId);
            createTaskAcknowledged();
          })
          .onError(createTaskFailed)
          .onImmediateRejection(createTaskFailed)
          .post();
    }

    return Promise.all(createTaskPromises);
  }

  /**
   * Sends a command to reassign the given task to the given user.
   *
   * @param {!TaskId} taskId
   * @param {!UserId} newAssignee
   * @param {!Client} client
   * @return {Promise<any>}
   */
  static reassignTask(taskId, newAssignee, client) {
    return new Promise((resolve, reject) => {
      const command = new ReassignTask();
      command.setId(taskId);
      command.setNewAssignee(newAssignee);

      client.command(command)
          .onOk(resolve)
          .onError(reject)
          .onImmediateRejection(reject)
          .post();
    })
  }

  /**
   * @param {?String} withName
   * @return {User}
   */
  static newUser(withName) {
    return {
      name: withName,
      id: TestEnvironment.userId(withName),
      tasks: []
    }
  }
}
