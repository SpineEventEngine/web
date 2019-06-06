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
import {fail, ensureUserTasks} from '../../test-helpers';
import {UserTasksTestEnvironment as TestEnvironment} from './given';
import {client} from '../given/firebase-client';
import {TypedMessage} from '@lib/index';
import {Filters} from '@lib/client/actor-request-factory';
import {UserTasks} from '@testProto/spine/web/test/given/user_tasks_pb';

/**
 * @typedef {Object} QueryTest a type representing a query test input parameters
 *                             and expected results
 *
 * @property {string} message a message describing test
 * @property {UserId[]} ids a list of IDs for query
 * @property {Filter[]} filters a list of filters for query
 * @property {User[]} expectedUsers a list of users expected to be received after
 *                                  execution of query
 */

describe('FirebaseClient executes query built', function () {
    let users;

    function allUserIds() {
        return users.map(user => TypedMessage.of(user.id));
    }

    /**
     * Prepares environment, where four users have one, two, three, and four tasks
     * assigned respectively.
     */
    before((done) => {
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
                setTimeout(() => {
                    done();
                }, 500);
            })
            .catch(() => fail(done));
        });

    /**
     * @type {QueryTest[]}
     */
    const tests = [
        {
            message: 'by ID',
            ids: () => users.slice(0, 1).map(user => TypedMessage.of(user.id)),
            expectedUsers: () => users.slice(0, 1)
        },
        {
            message: 'by IDs',
            ids: () => users.slice(0, 2).map(user => TypedMessage.of(user.id)),
            expectedUsers: () => users.slice(0, 2)
        },
        {
            message: 'by missing ID',
            ids: () => [
                TypedMessage.of(TestEnvironment.userId('user-without-tasks-assigned'))
            ],
            expectedUsers: () => users.slice(0, 2)
        },
        {
            message: 'with `eq` filter',
            filters: [
                Filters.eq('tasksCount', TypedMessage.int32(3))
            ],
            expectedUsers: () => users.filter(user => user.tasks.length === 3)
        },
        {
            message: 'with `lt` filter',
            filters: [
                Filters.lt('tasksCount', TypedMessage.int32(3))
            ],
            expectedUsers: () => users.filter(user => user.tasks.length < 3)
        },
        {
            message: 'with `gt` filter',
            filters: [
                Filters.gt('tasksCount', TypedMessage.int32(3))
            ],
            expectedUsers: () => users.filter(user => user.tasks.length > 3)
        },
        {
            message: 'with `le` filter',
            filters: [
                Filters.le('tasksCount', TypedMessage.int32(3))
            ],
            expectedUsers: () => users.filter(user => user.tasks.length <= 3)
        },
        {
            message: 'with `ge` filter',
            filters: [
                Filters.ge('tasksCount', TypedMessage.int32(3))
            ],
            expectedUsers: () => users.filter(user => user.tasks.length >= 3)
        },
        {
            message: 'with several filters applied to the same column',
            filters: [
                Filters.gt('tasksCount', TypedMessage.int32(1)),
                Filters.lt('tasksCount', TypedMessage.int32(3))
            ],
            expectedUsers: () => users.filter(user => user.tasks.length > 1 && user.tasks.length < 3)
        },
        {
            message: 'with several filters applied to different column',
            filters: [
                Filters.gt('tasksCount', TypedMessage.int32(1)),
                Filters.lt('overloaded', TypedMessage.bool(true))
            ],
            expectedUsers: () => users.filter(user => user.tasks.length > 1)
        },
        {
            message: 'with inappropriate filter',
            filters: [
                Filters.ge('tasksCount', TypedMessage.int32(100))
            ],
            expectedUsers: () => []
        }
    ];

    tests.forEach(test => {
        it(`${test.message} and returns correct values`, done => {

            const queryBuilder = client.newQuery()
                .select(UserTasks)
                .byIds(test.ids ? test.ids() : allUserIds());

            if (!!test.filters) {
                queryBuilder.where(test.filters)
            }

            const query = queryBuilder.build();

            client.execute(query)
                .then(userTasksList => {
                    assert.ok(ensureUserTasks(userTasksList, test.expectedUsers()));
                    done();
                })
                .catch(() => fail(done));
        });
    });
});
