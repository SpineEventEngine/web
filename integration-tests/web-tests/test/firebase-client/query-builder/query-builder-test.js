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
import TestEnvironment from '../given/test-environment';
import {client} from '../given/firebase-client';
import {TypedMessage} from '@lib/index';

describe('FirebaseClient query without criteria', function () {
    let user1, user2;
    let environmentPrepared;

    before(() => {
        user1 = TestEnvironment.newUser('query-tester1');
        user2 = TestEnvironment.newUser('query-tester2');
        const commandsSentPromise = TestEnvironment.createTwoTasksFor([user1, user2], client);

        environmentPrepared = new Promise(resolve => {
            commandsSentPromise.then(() => {
                // Gives time for the model state to be updated
                setTimeout(() => resolve(), 500);
            })
        })
    });

    it('by ID, at once -> list with single value', done => {
        environmentPrepared.then(() => {
            const typedUserId = TypedMessage.of(user1.id);
            const query = client._requestFactory.query()
                .select(TestEnvironment.TYPE.OF_ENTITY.USER_TASKS)
                .byIds([typedUserId])
                .build();

            client._fetchOf(query)
                .atOnce()
                .then(userTasksList => {
                    assert.ok(ensureUserTasks(userTasksList, [user1.id]));
                    done();
                })
                .catch(() => fail(done));
        });
    });

    it('by IDs, at once -> list with correct values', done => {
        environmentPrepared.then(() => {
            const typedUserId1 = TypedMessage.of(user1.id);
            const typedUserId2 = TypedMessage.of(user2.id);
            const query = client._requestFactory.query()
                .select(TestEnvironment.TYPE.OF_ENTITY.USER_TASKS)
                .byIds([typedUserId1, typedUserId2])
                .build();

            client._fetchOf(query)
                .atOnce()
                .then(userTasksList => {
                    assert.ok(ensureUserTasks(userTasksList, [user1.id, user2.id]));
                    done();
                })
                .catch(() => fail(done));
        });
    });

    it('by missing ID, at once -> empty list', done => {
        environmentPrepared.then(() => {
            const missingUserId = TestEnvironment.userId('user-without-tasks-assigned');

            const typedUserId = TypedMessage.of(missingUserId);
            const query = client._requestFactory.query()
                .select(TestEnvironment.TYPE.OF_ENTITY.USER_TASKS)
                .byIds([typedUserId])
                .build();

            client._fetchOf(query)
                .atOnce()
                .then(userTasksList => {
                    assert.equal(userTasksList.length, 0);
                    done();
                })
                .catch(e => fail(done, e));
        });
    });


    it('by ID, one by one -> single value', done => {
        environmentPrepared.then(() => {
            const typedUserId = TypedMessage.of(user1.id);
            const query = client._requestFactory.query()
                .select(TestEnvironment.TYPE.OF_ENTITY.USER_TASKS)
                .byIds([typedUserId])
                .build();

            const receivedItems = [];
            client._fetchOf(query)
                .oneByOne()
                .subscribe({
                    next: value => receivedItems.push(value),
                    error: fail(done),
                    complete: () => {
                        assert.ok(ensureUserTasks(receivedItems, [user1.id]));
                        done();
                    }
                });
        });
    });

    it('by IDs, one by one -> correct values', done => {
        environmentPrepared.then(() => {
            const typedUserId1 = TypedMessage.of(user1.id);
            const typedUserId2 = TypedMessage.of(user2.id);
            const query = client._requestFactory.query()
                .select(TestEnvironment.TYPE.OF_ENTITY.USER_TASKS)
                .byIds([typedUserId1, typedUserId2])
                .build();

            const receivedItems = [];
            client._fetchOf(query)
                .oneByOne()
                .subscribe({
                    next: value => receivedItems.push(value),
                    error: fail(done),
                    complete: () => {
                        assert.ok(ensureUserTasks(receivedItems, [user1.id, user2.id]));
                        done();
                    }
                });
        });
    });

    it('by missing ID, one by one -> no values', done => {
        environmentPrepared.then(() => {
            const missingUserId = TestEnvironment.userId('user-without-tasks-assigned');

            const typedUserId = TypedMessage.of(missingUserId);
            const query = client._requestFactory.query()
                .select(TestEnvironment.TYPE.OF_ENTITY.USER_TASKS)
                .byIds([typedUserId])
                .build();

            const receivedItems = [];
            client._fetchOf(query)
                .oneByOne()
                .subscribe({
                    next: value => receivedItems.push(value),
                    error: fail(done),
                    complete: () => {
                        assert.equal(receivedItems.length, 0);
                        done();
                    }
                });
        });
    });
});
