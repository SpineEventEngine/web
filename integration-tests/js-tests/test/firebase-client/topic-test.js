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

import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {ensureUserTasksCount, fail, toListObservable} from '../test-helpers';
import {UserTasksTestEnvironment as TestEnvironment} from '../given/users-tasks-test-environment';
import {client} from './given/firebase-client';
import {Filters} from '@lib/client/actor-request-factory';
import {UserTasks} from '@testProto/spine/web/test/given/user_tasks_pb';

/**
 * A utility class that allows to control over the state of `UserTasks` observable
 * and to perform actions when the `UserTasks` list matches the next expected state. The configuration
 * of the expected states and followed actions can be done in a Promise-like manner:
 *
 * ```
 *      UserTasksFlow.for(userTasksList$)
 *          .waitFor([])
 *          .waitFor([
 *              { id: user1.id, tasksCount: 1 },
 *          ])
 *          .then(() => assignTaskTo(user1))
 *          .waitFor([
 *              { id: user1.id, tasksCount: 2 },
 *          ])
 *          .then(() => assignTaskTo(user2))
 *          .waitFor([
 *              { id: user1.id, tasksCount: 2 },
 *              { id: user2.id, tasksCount: 1 },
 *          ]).start()
 * ```
 */
class UserTasksFlow {

  constructor(userTasksList$) {
    this.userTasksList$ = userTasksList$;
    this._states = [];
    this._transitions = [];
  }

  static for(userTasksList$) {
    return new UserTasksFlow(userTasksList$);
  }

  /**
   * Adds a transition that does nothing if the next state is expected to be received
   * without any actions. This allows to handle such cases:
   * ```
   *      ...
   *      .waitFor([]) // The NoOp transition will be performed when this state matches
   *      .waitFor([
   *          { id: user1.id, tasksCount: 1 }
   *      ])
   *      ...
   * ```
   * @private
   */
  static _equalize(states, transitions) {
    if (states.length === transitions.length) {
      return;
    }
    if (states.length === transitions.length + 1) {
      transitions.push(UserTasksFlow.doNothing);
      return;
    }

    throw new Error('The consistency of the flow is broken. Ensure each action is' +
        'followed by the next expected state.');
  }

  static doNothing() {
    // NO-OP.
  }

  waitFor(state) {
    UserTasksFlow._equalize(this._states, this._transitions);
    this._states.push(state);
    return this;
  }

  then(perform) {
    this._transitions.push(perform);
    return this;
  }

  /**
   * Starts to observe the user tasks list and perform actions when the
   * list state matches the next expected state.
   *
   * @return {Promise} a promise to be resolved when all expected states were
   *      received and all followed
   */
  start() {
    return new Promise((resolve, reject) => {
      if (!this._states.length) {
        reject('The list of states must not be empty.');
      }
      UserTasksFlow._equalize(this._states, this._transitions);
      const subscriptionEnd$ = new Subject();
      this.userTasksList$
          .pipe(takeUntil(subscriptionEnd$))
          .subscribe(userTasksList => {
            const nextState = this._states[0];

            const nextStateMatches = ensureUserTasksCount(userTasksList, nextState);
            try {
              this._transitIf(nextStateMatches);
            } catch (e) {
              reject(e);
            }

            if (this._hasNoMoreStates()) {
              subscriptionEnd$.next();
              subscriptionEnd$.complete();
              resolve();
            }
          });
    });
  }

  _transitIf(stateMatches) {
    if (stateMatches) {
      this._states.shift();
      const performTransition = this._transitions.shift();
      performTransition();
    }
  }

  _hasNoMoreStates() {
    return !this._states.length;
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

  it('built by IDs and retrieves correct data', (done) => {
    client.subscribeTo(UserTasks)
        .byId([user1.id, user2.id])
        .post()
        .then(subscription => {
          teardownSubscription = subscription.unsubscribe;
          const userTasksList$ = toListObservable(subscription, compareUserTasks);
          const userTasksFlow = UserTasksFlow.for(userTasksList$);

          userTasksFlow
              .waitFor([
                {id: user1.id, tasksCount: 2},
                {id: user2.id, tasksCount: 2}
              ])
              .start()
              .then(done)
              .catch(e => fail(done, e)())
        })
  });

  it('built by IDs and filters and retrieves correct data', (done) => {
    client.subscribeTo(UserTasks)
        .byId([user1.id, user2.id])
        .where(Filters.eq('task_count', 2))
        .post()
        .then(subscription => {
          teardownSubscription = subscription.unsubscribe;
          const userTasksList$ = toListObservable(subscription, compareUserTasks);
          const userTasksFlow = UserTasksFlow.for(userTasksList$);

          userTasksFlow
              .waitFor([
                {id: user1.id, tasksCount: 2},
                {id: user2.id, tasksCount: 2}
              ])
              .start()
              .then(done)
              .catch(e => fail(done, e)())
        })
  });

  it('built by IDs and filters and updates data correctly when state changes', (done) => {
    client.subscribeTo(UserTasks)
        .byId([user1.id, user2.id])
        .where(Filters.ge('task_count', 2))
        .post()
        .then(subscription => {
          teardownSubscription = subscription.unsubscribe;
          const userTasksList$ = toListObservable(subscription, compareUserTasks);
          const userTasksFlow = UserTasksFlow.for(userTasksList$);

          userTasksFlow
              .waitFor([
                {id: user1.id, tasksCount: 2},
                {id: user2.id, tasksCount: 2}
              ])
              .then(() => {
                const taskToReassign = user1.tasks[0];
                TestEnvironment.reassignTask(taskToReassign, user2.id, client);
              })
              .waitFor([
                {id: user2.id, tasksCount: 3}
              ])
              .then(() => {
                const taskToReassign = user1.tasks[1];
                TestEnvironment.reassignTask(taskToReassign, user2.id, client);
              })
              .waitFor([
                {id: user2.id, tasksCount: 4}
              ])
              .start()
              .then(done)
              .catch(e => fail(done, e)())
        });
  });
});
