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
import {ensureUserTasks, fail} from '../test-helpers';
import {UserTasksTestEnvironment as TestEnvironment} from '../given/users-tasks-test-environment';
import {client} from './given/firebase-client';
import {enumValueOf, Filters} from '@lib/client/actor-request-factory';
import {TypedMessage} from '@lib/client/typed-message';
import {BoolValue} from '@testProto/google/protobuf/wrappers_pb';
import {UserTasks} from '@testProto/spine/web/test/given/user_tasks_pb';

/**
 * @typedef {Object} QueryTest an object representing a FirebaseClient query test input
 *                             parameters and expected results
 *
 * @property {string} message a message describing test
 * @property {UserId[]} ids a list of IDs for query
 * @property {Filter[]} filters a list of filters for query
 * @property {User[]} expectedUsers a list of users expected to be received after
 *                                  execution of query
 */

describe('FirebaseClient executes query built', function () {
  let users;

  function toUserIds(users) {
    return users.map(user => user.id);
  }

  /**
   * Prepares environment, where four users have one, two, three, and four tasks
   * assigned respectively.
   */
  before(function (done) {
    // Big timeout allows complete environment setup.
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

  /**
   * @type {QueryTest[]}
   */
  const tests = [
    {
      message: 'by ID',
      ids: () => toUserIds(users.slice(0, 1)),
      expectedUsers: () => users.slice(0, 1)
    },
    {
      message: 'by IDs',
      ids: () => toUserIds(users.slice(0, 2)),
      expectedUsers: () => users.slice(0, 2)
    },
    {
      message: 'by missing ID',
      ids: () => [
        TestEnvironment.userId('user-without-tasks-assigned')
      ],
      expectedUsers: () => users.slice(0, 2)
    },
    {
      message: 'with `eq` filter',
      filters: [
        Filters.eq('task_count', 3)
      ],
      expectedUsers: () => users.filter(user => user.tasks.length === 3)
    },
    {
      message: 'with `eq` filter targeting a enum column',
      filters: [
        Filters.eq('load', enumValueOf(UserTasks.Load.VERY_HIGH))
      ],
      expectedUsers: () => users.filter(user => user.tasks.length > 1)
    },
    {
      message: 'with `lt` filter',
      filters: [
        Filters.lt('task_count', 3)
      ],
      expectedUsers: () => users.filter(user => user.tasks.length < 3)
    },
    {
      message: 'with `gt` filter',
      filters: [
        Filters.gt('task_count', 3)
      ],
      expectedUsers: () => users.filter(user => user.tasks.length > 3)
    },
    {
      message: 'with `le` filter',
      filters: [
        Filters.le('task_count', TypedMessage.int32(3))
      ],
      expectedUsers: () => users.filter(user => user.tasks.length <= 3)
    },
    {
      message: 'with `ge` filter',
      filters: [
        Filters.ge('task_count', 3)
      ],
      expectedUsers: () => users.filter(user => user.tasks.length >= 3)
    },
    {
      message: 'with several filters applied to the same column',
      filters: [
        Filters.gt('task_count', 1),
        Filters.lt('task_count', 3)
      ],
      expectedUsers: () => users.filter(user => user.tasks.length > 1 && user.tasks.length < 3)
    },
    {
      message: 'with several filters applied to different column',
      filters: [
        Filters.gt('task_count', 1),
        Filters.lt('is_overloaded', new BoolValue([true]))
      ],
      expectedUsers: () => users.filter(user => user.tasks.length > 1)
    },
    {
      message: 'with inappropriate filter',
      filters: [
        Filters.ge('task_count', 100)
      ],
      expectedUsers: () => []
    }
  ];

  tests.forEach(test => {
    it(`${test.message} and returns correct values`, done => {
      const ids = test.ids ? test.ids() : toUserIds(users);
      const filters = test.filters;

      client.select(UserTasks)
          .byId(ids)
          .where(filters)
          .run()
          .then(userTasksList => {
            assert.ok(ensureUserTasks(userTasksList, test.expectedUsers()));
            done();
          })
          .catch(() => fail(done));
    });
  });

  it('with `Date`-based filter and returns correct values', (done) => {
    const userIds = toUserIds(users);

    client.select(UserTasks)
        .byId(userIds)
        .run()
        .then(data => {
          assert.ok(Array.isArray(data));
          assert.strictEqual(data.length, userIds.length);

          const firstUserTasks = data[0];

          const lastUpdatedTimestamp = firstUserTasks.getLastUpdated();
          const seconds = lastUpdatedTimestamp.getSeconds();
          const nanos = lastUpdatedTimestamp.getNanos();
          const millis = seconds * 1000 + nanos / 1000000;
          const flukeMillis = 1;

          client.select(UserTasks)
              .where([Filters.gt('last_updated', new Date(millis - flukeMillis)),
                      Filters.lt('last_updated', new Date(millis + flukeMillis))])
              .run()
              .then(userTasksList => {
                assert.ok(Array.isArray(userTasksList));
                assert.strictEqual(userTasksList.length, 1);

                const actualUserId = userTasksList[0].getId();
                assert.strictEqual(actualUserId.getValue(), firstUserTasks.getId().getValue());
                done();
              })
              .catch((e) => {
                console.error("Failed 2nd query: " + e);
                fail(done);
              });
        })
        .catch((e) => {
          console.error("Failed 1st query: " + e);
          fail(done);
        });
  });
});
