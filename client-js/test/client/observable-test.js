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

// noinspection NodeJsCodingAssistanceForCoreModules
import assert from "assert";

import {Observable} from "../../src/client/observable";

const MILLISECONDS = 1;
const SECONDS = 1000 * MILLISECONDS;

describe("Observable should", function () {

  this.timeout(5 * SECONDS);

  it("send next values to the observer", done => {
    const observable = new Observable(observer => {
      observer.next(1);
      observer.next(2);
      setTimeout(() => {
        observer.next(3);
        observer.complete();
      });
    });

    const retrievedValues = [];
    observable.subscribe({
      next(value) {
        retrievedValues.push(value);
      },
      complete() {
        assert.equal(retrievedValues.length, 3);
        assert.equal(retrievedValues[0], 1);
        assert.equal(retrievedValues[1], 2);
        assert.equal(retrievedValues[2], 3);
        done();
      }
    })
  });

  it("send an error to the observer", done => {
    const expectedError = new Error("An observable error.");
    const observable = new Observable(observer => {
      observer.next(1);
      setTimeout(() => {
        observer.next(2);
      });
      throw expectedError;
    });

    const retrievedValues = [];
    observable.subscribe({
      next(value) {
        retrievedValues.push(value);
      },
      error(error) {
        assert.ok(retrievedValues.length === 1);
        assert.ok(retrievedValues[0] === 1);
        assert.ok(error === expectedError);
        done();
      }
    });
  });
});
