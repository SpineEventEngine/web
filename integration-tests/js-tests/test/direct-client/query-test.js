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

import assert from 'assert';
import {ensureUserTasks, fail} from '../test-helpers';
import {UserTasksTestEnvironment as TestEnvironment} from '../given/users-tasks-test-environment';
import {client} from './given/direct-client';
import {UserTasks} from '@testProto/spine/web/test/given/user_tasks_pb';

describe('DirectClient executes query built', function () {

  let users;

  /**
   * Prepares environment, where four users have one, two, three, and four tasks
   * assigned respectively.
   */
  before(function (done) {
    // Big timeout allows to complete environment setup.
    this.timeout(10 * 1000);

    users = [
      TestEnvironment.newUser('query-tester1'),
      TestEnvironment.newUser('query-tester2'),
      TestEnvironment.newUser('query-tester3'),
      TestEnvironment.newUser('query-tester4')
    ];

    const createTasksPromises = [];
    users.forEach((user, index) => {
      const tasksCount = index + 1;
      const promise = TestEnvironment.createTaskFor(user, tasksCount, client);
      createTasksPromises.push(promise);
    });
    Promise.all(createTasksPromises)
        .then(() => {
          // Gives time for the model state to be updated
          setTimeout(done, 500);
        });
  });

  it(`by IDs and returns correct values`, done => {
    const ids = users.map(user => user.id);
    client.select(UserTasks)
        .byId(ids)
        .run()
        .then(userTasksList => {
          assert.ok(ensureUserTasks(userTasksList, users));
          done();
        })
        .catch(fail(done));
  });
});
