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

"use strict";

import {Timestamp} from '../proto/google/protobuf/timestamp_pb';

/**
 * @typedef {Object} DurationValue
 *
 * @property {number} seconds
 * @property {number} minutes
 */

const NANOSECONDS_IN_MILLISECOND = 1000;
const MILLISECONDS_IN_SECOND = 1000;

const SECONDS_IN_MINUTE = 60;
const MILLISECONDS_IN_MINUTE = SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND;

const MINUTES_IN_HOUR = 60;
const SECONDS_IN_HOUR = MINUTES_IN_HOUR * SECONDS_IN_MINUTE;
const MILLISECONDS_IN_HOUR = SECONDS_IN_HOUR * MILLISECONDS_IN_SECOND;

const HOURS_IN_DAY = 24;
const MINUTES_IN_DAY = MINUTES_IN_HOUR * HOURS_IN_DAY;
const SECONDS_IN_DAY = MINUTES_IN_DAY * SECONDS_IN_MINUTE;
const MILLISECONDS_IN_DAY = SECONDS_IN_DAY * MILLISECONDS_IN_SECOND;

/**
 * Checks that each item in provided items is non-negative. An error is thrown otherwise.
 *
 * @param {!number[]} items an array of numbers to check
 * @param {!String} message a message for when one of the items does not match the requirement
 */
function checkItemsNotNegative(items, message) {
  items.forEach(item => {
    if (item < 0) {
      throw new Error(message);
    }
  });
}

/**
 * Converts a given JavaScript Date into the Timestamp Protobuf message.
 *
 * @param {!Date} date a date to convert
 * @return {Timestamp} a timestamp message of the given date value
 *
 * @throws error when non-Date value is passed or it is invalid
 */
export function convertDateToTimestamp(date) {
  const errorMessage = (message) => `Cannot convert to Timestamp. ${message}`;

  if (!(date instanceof Date && typeof date.getTime === 'function')) {
    throw new Error(errorMessage(`The given "${date}" isn't of Date type.`));
  }

  if (isNaN(date.getTime())) {
    throw new Error(errorMessage(`The given "${date}" is invalid.`));
  }
  const millis = date.getTime();

  const timestamp = new Timestamp();
  timestamp.setSeconds(Math.trunc(millis / 1000));
  timestamp.setNanos((millis % 1000) * 1000000);
  return timestamp;
}


/**
 * A duration of a specified amount of time.
 *
 * A duration can not be negative, throwing an error if any of the provided values are less than 0.
 */
export class Duration {

  /**
   * @param {?number} nanoseconds a number of nanoseconds in addition to all other values
   * @param {?number} milliseconds a number of milliseconds in addition to all other values
   * @param {?number} seconds a number of seconds in addition to all other values
   * @param {?number} minutes a number of minutes in addition to all other values
   * @param {?number} hours a number of hours in addition to all other values
   * @param {?number} days a number of days in addition to all other values
   */
  constructor({nanoseconds, milliseconds, seconds, minutes, hours, days}) {
    checkItemsNotNegative(
      [nanoseconds, milliseconds, seconds, minutes, hours, days],
      'Duration cannot be negative'
    );
    this._nanoseconds = nanoseconds;
    this._milliseconds = milliseconds;
    this._seconds = seconds;
    this._minutes = minutes;
    this._hours = hours;
    this._days = days;
  }

  /**
   * @return {DurationValue} a value provided to the `Duration` constructor
   */
  value() {
    return {
      nanoseconds: this._nanoseconds,
      milliseconds: this._milliseconds,
      seconds: this._seconds,
      minutes: this._minutes,
      hours: this._hours,
      days: this._days,
    };
  }

  /**
   * @return {number} a total number of ms of this Duration value amounts as an integer
   */
  inMs() {
    let total = 0;
    if (this._nanoseconds) {
      total += this._nanoseconds / NANOSECONDS_IN_MILLISECOND;
    }
    if (this._milliseconds) {
      total += this._milliseconds;
    }
    if (this._seconds) {
      total += this._seconds * MILLISECONDS_IN_SECOND;
    }
    if (this._minutes) {
      total += this._minutes * MILLISECONDS_IN_MINUTE;
    }
    if (this._hours) {
      total += this._hours * MILLISECONDS_IN_HOUR;
    }
    if (this._days) {
      total += this._days * MILLISECONDS_IN_DAY;
    }
    return Math.floor(total);
  }
}
