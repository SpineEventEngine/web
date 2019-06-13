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

import {Subject, Observable} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {fail, ensureUserTasksCount, toListObservable} from '../test-helpers';
import {UserTasksTestEnvironment as TestEnvironment} from './given/users-test-environment';
import {client} from './given/firebase-client';
import {Filters} from '@lib/client/actor-request-factory';
import {UserTasks} from '@testProto/spine/web/test/given/user_tasks_pb';

/**
 * A utility class that allows to convert `UserTasks` subscription
 * and to check the consistency of the state updates.
 */
class UserTasksSubscriptions {

    /**
     * Subscribes to the given user tasks list, compares each next state with
     * the next expected state. Fails the given `Done` test function if the state mismatch occurs,
     * resolves `Done` function when the list of expected states becomes empty.
     *
     * @param {Observable<UserTasks[]>} userTasksList$
     * @param {{
     *     id: UserId,
     *     tasksCount: number
     * } | null []} expectedStates the list of expected user tasks states,the  not-`null` value in
     *              the list indicates that the state check on this step must be performed
     * @param done `Done` function to complete/fail async test
     */
    static ensureStateUpdates(userTasksList$, expectedStates, done) {
        if (!expectedStates.length) {
            throw new Error('The list of states must not be empty.');
        }
        const subscriptionEnd$ = new Subject();
        userTasksList$
            .pipe(takeUntil(subscriptionEnd$))
            .subscribe(userTasksList => {
                const expectedState = expectedStates[0];

                const stateMatches = ensureUserTasksCount(userTasksList, expectedState);
                if (stateMatches) {
                    expectedStates.shift();
                }

                if (expectedStates.length === 0) {
                    subscriptionEnd$.next();
                    subscriptionEnd$.complete();
                    done();
                }
        });
    }
}

describe('FirebaseClient subscribes to topic', function () {
    // Big timeout allows to receive model state changes during tests.
    this.timeout(120 * 1000);
    const compareUserTasks = (userTasks1, userTasks2) =>
        userTasks1.getId().getValue() === userTasks2.getId().getValue();

    let user1;
    let user2;
    let teardownSubscription = () => {
    };

    /**
     * Prepares environment where two users have two tasks assigned each.
     */
    beforeEach((done) => {
        user1 = TestEnvironment.newUser('topic-tester-1');
        user2 = TestEnvironment.newUser('topic-tester-2');
        teardownSubscription = () => {};

        const createTasksPromises = [];
        [user1, user2].forEach(user => {
            const promise = TestEnvironment.createTaskFor(user, 2, client);
            createTasksPromises.push(promise);
        });
        Promise.all(createTasksPromises)
            .then(() => done());
    });

    afterEach(() => {
        teardownSubscription();
    });

    function buildTopicFor({ids, filters}) {
        const topicBuilder = client.newTopic()
            .select(UserTasks)
            .byIds(ids);

        if (!!filters) {
            topicBuilder.where(filters)
        }

        return topicBuilder.build();
    }

    it('built by IDs and retrieves correct data', (done) => {
        const topic = buildTopicFor({ids: [user1.id, user2.id]});

        client.subscribeTo(topic)
            .then(subscription => {
                teardownSubscription = subscription.unsubscribe;
                const userTasksList$ = toListObservable(subscription, compareUserTasks);
                UserTasksSubscriptions.ensureStateUpdates(userTasksList$, [
                    [],
                    [
                        { id: user1.id, tasksCount: 2 },
                        { id: user2.id, tasksCount: 2 }
                    ]
                ], done)
            })
    });

    it('built by IDs and filters and retrieves correct data', (done) => {
        const topic = buildTopicFor({
            ids: [user1.id, user2.id],
            filters: [
                Filters.eq('tasksCount', 2)
            ]
        });

        client.subscribeTo(topic)
            .then(subscription => {
                teardownSubscription = subscription.unsubscribe;
                const userTasksList$ = toListObservable(subscription, compareUserTasks);
                UserTasksSubscriptions.ensureStateUpdates(userTasksList$, [
                    [],
                    [
                        { id: user1.id, tasksCount: 2 },
                        { id: user2.id, tasksCount: 2 }
                    ]
                ], done)
            })
    });

    it('built by IDs and filters and updates data correctly when state changes', (done) => {
        const topic = buildTopicFor({
            ids: [user1.id, user2.id],
            filters: [
                Filters.ge('tasksCount', 2)
            ]
        });

        client.subscribeTo(topic)
            .then(subscription => {
                teardownSubscription = subscription.unsubscribe;
                const userTasksList$ = toListObservable(subscription, compareUserTasks);
                UserTasksSubscriptions.ensureStateUpdates(userTasksList$, [
                    [],
                    [
                        { id: user1.id, tasksCount: 2 },
                        { id: user2.id, tasksCount: 2 }
                    ],
                    [
                        { id: user2.id, tasksCount: 3 }
                    ],
                ], done)
            });

            const taskToReassign = user1.tasks[0];
            TestEnvironment.reassignTask(taskToReassign, user2.id, client);
    });
});
