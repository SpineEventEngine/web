/*
 * Copyright 2018, TeamDev. All rights reserved.
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

"use strict";

import {Subscription} from './observable';

/**
 * The client of a Firebase Realtime database.
 */
export class FirebaseClient {

  /**
   * Creates a new FirebaseClient.
   *
   * @param {!firebase.app.App} firebaseApp an initialized Firebase app
   */
  constructor(firebaseApp) {
    this._firebaseApp = firebaseApp;
  }

  /**
   * Subscribes to the `child_added` events of of the node under the given path.
   *
   * Each child's value is parsed as a JSON and dispatched to the given callback
   *
   * @param {!string} path the path to the watched node
   * @param {!consumerCallback<Object>} dataCallback the child value callback
   *
   * @return {Subscription} a Subscription that can be unsubscribed
   */
  onChildAdded(path, dataCallback) {
    return this._subscribeToChildEvent('child_added');
  }

  /**
   * Subscribes to the `child_changed` events of of the node under the given path.
   *
   * Each child's value is parsed as a JSON and dispatched to the given callback
   *
   * @param {!string} path the path to the watched node
   * @param {!consumerCallback<Object>} dataCallback the child value callback
   *
   * @return {Subscription} a Subscription that can be unsubscribed
   */
  onChildChanged(path, dataCallback) {
    return this._subscribeToChildEvent('child_changed', path, dataCallback);
  }

  _subscribeToChildEvent(childEvent, path, dataCallback) {
    const dbRef = this._firebaseApp.database().ref(path);
    const callback = dbRef.on(childEvent, response => {
      const msgJson = response.val();
      const message = JSON.parse(msgJson);
      dataCallback(message);
    });
    return new Subscription(() => {
      dbRef.off(childEvent, callback);
    });
  }

  /**
   * Subscribes to the `child_removed` events of of the node under the given path.
   *
   * Each child's value is parsed as a JSON and dispatched to the given callback
   *
   * @param {!string} path the path to the watched node
   * @param {!consumerCallback<Object>} dataCallback the child value callback
   *
   * @return {Subscription} a Subscription that can be unsubscribed
   */
  onChildRemoved(path, dataCallback) {
    return this._subscribeToChildEvent('child_removed', path, dataCallback);
  }


  /**
   * Gets the value from Firebase at the provided path.
   *
   * @param {!string} path the path to the node to get value from
   * @param {!consumerCallback<Object[]>} dataCallback a callback which is invoked with an array of
   *                                                   entities at path
   */
  getValue(path, dataCallback) {
    const dbRef = this._firebaseApp.database().ref(path);
    dbRef.once('value', response => {
      const data = response.val(); // an Object mapping Firebase ids to objects is returned
      const objectStrings = Object.values(data);
      const items = objectStrings.map(item => JSON.parse(item));
      dataCallback(items);
    });
  }
}
