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

describe('FirebaseClient query returns correct values', function () {
    let user1, user2, user3, user4;
    let environmentPrepared;

    function typedUserIds() {
        return [user1, user2, user3, user4].map(user => TypedMessage.of(user.id));
    }

    /**
     * Prepares environment, where four users have one, two, three, and four tasks
     * assigned respectively.
     */
    before(() => {
        user1 = TestEnvironment.newUser('filtered-query-tester1');
        user2 = TestEnvironment.newUser('filtered-query-tester2');
        user3 = TestEnvironment.newUser('filtered-query-tester3');
        user4 = TestEnvironment.newUser('filtered-query-tester4');

        const createTasksPromises = [];
        [user1, user2, user3, user4].forEach((user, index) => {
            const tasksCount = index + 1;
            const promise = TestEnvironment.createTaskFor(user, tasksCount, client);
            createTasksPromises.push(promise);
        });

        environmentPrepared = new Promise(resolve => {
            Promise.all(createTasksPromises).then(() => {
                // Gives time for the model state to be updated
                setTimeout(() => resolve(), 500);
            })
        })
    });

    it('with `eq` filter, at once', done => {
        environmentPrepared.then(() => {
            const query = client._requestFactory.query()
                .select(TestEnvironment.TYPE.OF_ENTITY.USER_TASKS)
                .byIds(typedUserIds())
                .where([
                    Filters.eq('tasksCount', TypedMessage.int32(user3.tasks.length))
                ])
                .build();

            client._fetchOf(query)
                .atOnce()
                .then(userTasksList => {
                    assert.ok(ensureUserTasks(userTasksList, [user3.id]));
                    done();
                })
                .catch(() => fail(done));
        });
    });

    it('with inappropriate filter, at once', done => {
        environmentPrepared.then(() => {
            const query = client._requestFactory.query()
                .select(TestEnvironment.TYPE.OF_ENTITY.USER_TASKS)
                .byIds(typedUserIds())
                .where([
                    Filters.eq('tasksCount', TypedMessage.int32(100))
                ])
                .build();

            client._fetchOf(query)
                .atOnce()
                .then(userTasksList => {
                    assert.ok(ensureUserTasks(userTasksList, []));
                    done();
                })
                .catch(() => fail(done));
        });
    });
});
