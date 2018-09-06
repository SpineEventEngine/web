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

/**
 * @typedef {Object} DurationValue
 *
 * @property {number} seconds
 * @property {number} minutes
 */

/**
 * A duration of a specified amount of seconds or minutes.
 */
export class Duration {

  /**
   * @param {?number} seconds a number of seconds in this duration in addition to all other values
   * @param {?number} minutes a number of minutes in this duration in addition to all other values
   */
  constructor({seconds, minutes}) {
    this._seconds = seconds;
    this._minutes = minutes;
  }

  /**
   * @return {DurationValue} a value provided to the `Duration` constructor
   */
  value() {
    return {
      seconds: this._seconds,
      minutes: this._minutes
    };
  }

  /**
   * @return {number} a total number of ms of this Duration value amounts
   */
  inMs() {
    let total = 0;
    if (this._seconds) {
      total += this._seconds * MILLISECONDS_IN_SECOND;
    }
    if (this._minutes) {
      total += this._minutes * MILLISECONDS_IN_MINUTE;
    }
    return total;
  }
}

const MILLISECONDS_IN_SECOND = 1000;

const SECONDS_IN_MINUTE = 60;
const MILLISECONDS_IN_MINUTE = SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND;
