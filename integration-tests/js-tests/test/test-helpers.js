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

import {BehaviorSubject, Observable} from 'rxjs';

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
 * Ensures given list of `UserTasks` contains items with expected IDs and with
 * an expected tasks count.
 *
 * @param {UserTasks[]} actualUserTasks
 * @param {{
 *     id: UserId,
 *     tasksCount: number
 * }[]} expectedUsers
 */
export function ensureUserTasksCount(actualUserTasks, expectedUsers) {
  return arraysEqualDeep(actualUserTasks,
      expectedUsers,
      (userTasks, expected) =>
          userTasks.getId().getValue() === expected.id.getValue()
          && userTasks.getTasksList().length === expected.tasksCount
  );
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
  if (!Array.isArray(arr1) || !Array.isArray(arr2)) {
    throw new Error('Unable to compare equality of non-array objects.');
  }

  if (arr1.length === 0 && arr2.length === 0) {
    return true;
  }

  const intersection = arr1.filter(value1 => {
    return arr2.findIndex(value2 => compare(value1, value2)) > -1;
  });

  return intersection.length === arr1.length
}

/**
 * Composes the given subscription object into the list observable.
 *
 * @param {EntitySubscriptionObject<T>} subscription a subscription to retrieve values
 * @param {(o1: T, o2: T) => boolean} compare a function that compares objects of `T` type;
 *      returns `true` if objects are considered equal, `false` otherwise
 * @return {Observable<T[]>} an observable that emits a list of values, composed of the given
 *      subscription object
 *
 * @template <T> a class of a subscription target entities
 */
export function toListObservable(subscription, compare) {
  const list$ = new BehaviorSubject([]);
  const {itemAdded, itemChanged, itemRemoved} = subscription;

  itemAdded.subscribe({
    next: addedItem => {
      const currentList = list$.getValue();
      list$.next([...currentList, addedItem]);
    }
  });

  itemChanged.subscribe({
    next: changedItem => {
      const currentList = list$.getValue();
      const changedItemIndex = _indexOf(changedItem, currentList, compare);
      const updatedList = currentList.slice();
      updatedList[changedItemIndex] = changedItem;
      list$.next(updatedList);
    }
  });

  itemRemoved.subscribe({
    next: removedItem => {
      const currentList = list$.getValue();
      const removedItemIndex = _indexOf(removedItem, currentList, compare);
      const updatedList = [
        ...currentList.slice(0, removedItemIndex),
        ...currentList.slice(removedItemIndex + 1)
      ];
      list$.next(updatedList);
    }
  });

  return list$.asObservable();
}

function _indexOf(item, items, compare) {
  return items.findIndex(value => compare(value, item));
}
