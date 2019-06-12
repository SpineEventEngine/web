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

/**
 * A helper functions to fail fast async tests.
 *
 * Can be used in callback-based async tests to fail them before waiting
 * of the full test timeout.
 *
 * @example
 * // To fail the test when checking Promise that should be rejected
 * promiseThatShouldBeRejected
 *   .then(fail(done, 'Expecting promise to be rejected'))
 *   .catch(error => {
 *     checkError(error);
 *     done();
 *    });
 * @example
 * // To fail the test when checking Promise that should be resolved
 * promiseThatShouldBeResolved
 *   .then(value => {
 *     checkValue(value);
 *     done();
 *    })
 *   .catch(fail(done))
 *
 * @param {function(*=)} done the callback that should be called when your test is complete
 * @param {string=} message the test failure message
 * @return {function(*=)} a function to fail the test. Accepts the first parameter as a cause for test failure.
 */
export function fail(done, message = '') {
  return cause => {
    if (message) {
     done(new Error(`Test failed. Cause: ${message}`));
    } else {
     done(new Error(`Test failed. Cause: ${cause ? cause : 'not identified'}`));
    }
  };
}

/**
 * Ensures given lists contain the same user IDs.
 *
 * @param {UserId[]} actual
 * @param {UserId[]} expected
 */
export function ensureUserIds(actual, expected) {
    return arraysEqualDeep(actual, expected, (userId1, userId2) =>
        userId1.getValue() === userId2.getValue());
}

/**
 * Ensures given list of `UserTasks` contains items with expected IDs.
 *
 * @param {UserTasks[]} actualUserTasks
 * @param {{
 *     id: UserId,
 *     tasks: TaskId[]
 * }[]} expectedUsers
 */
export function ensureUserTasks(actualUserTasks, expectedUsers) {
    const actualUserIds = actualUserTasks.map(userTasks => userTasks.getId());
    const expectedUserIds = expectedUsers.map(user => user.id);
    return ensureUserIds(actualUserIds, expectedUserIds);
}

/**
 * Ensures given arrays have the same elements. Uses given function
 * to compare arrays elements.
 *
 * @param {Object[]}arr1
 * @param {Object[]} arr2
 * @param {(o1, o2) => boolean} compare compares objects of type of arrays entries
 * @return {boolean} `true` if arrays are equal; `false` otherwise;
 */
function arraysEqualDeep(arr1, arr2, compare) {
    const intersection = arr1.filter(value1 => {
        return arr2.findIndex(value2 => compare(value1, value2)) > -1;
    });

    return intersection.length === arr1.length
}
