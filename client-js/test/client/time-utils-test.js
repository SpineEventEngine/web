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

import assert from 'assert';
import {convertDateToTimestamp} from '@lib/client/time-utils';

describe(`"convertDateToTimestamp" function`, () => {

  const errorPrefix = 'Cannot convert to Timestamp.';

  it('throws a respective error when non-Date value passed', () => {
    const nonDateValue = 'today at 14 pm';
    const expectedErrorMessage =
        `${errorPrefix} The given "${nonDateValue}" isn't of Date type.`;
    assert.throws(() => convertDateToTimestamp(nonDateValue), null, expectedErrorMessage);
  });

  it('throws a respective error when invalid Date value passed', () => {
    const invalidDate = new Date('the day when I get rich');
    const expectedErrorMessage =
        `${errorPrefix} The given "${invalidDate}" is invalid.`;

    assert.throws(() => convertDateToTimestamp(invalidDate), null, expectedErrorMessage);
  });

  it('converts to Timestamp correctly', () => {
    const now = new Date(Date.now());
    const timestamp = convertDateToTimestamp(now);

    const actualSeconds = timestamp.getSeconds();
    const actualNanos = timestamp.getNanos();
    const expectedSeconds = Math.trunc(now.getTime() / 1000);
    const expectedNanos = (now.getTime() % 1000) * 1000000;

    assert.strictEqual(actualSeconds, expectedSeconds);
    assert.strictEqual(actualNanos, expectedNanos);
  });
});
