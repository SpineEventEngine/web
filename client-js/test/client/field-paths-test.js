/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import assert from 'assert';

import {FieldPaths} from '@lib/client/field-paths';
import {fail} from './test-helpers';

describe('FieldPaths', () => {

  it('throws an Error on construction', done => {
    try {
      new FieldPaths();
      fail(done, "FieldPaths utility constructor didn't throw an error")
    } catch (ignored) {
    }
    done();
  });

  it('creates a FieldPath from a simple path string', () => {
    const pathStr = "fieldPath";
    const fieldPath = FieldPaths.parse(pathStr);
    assert.strictEqual(fieldPath.getFieldNameList()[0], pathStr);
  });

  it('creates a FieldPath from a composite path string', () => {
    const pathStr = "field1.field2";
    const fieldPath = FieldPaths.parse(pathStr);
    assert.strictEqual(fieldPath.getFieldNameList()[0], "field1");
    assert.strictEqual(fieldPath.getFieldNameList()[1], "field2");
  });

  it('throws on parsing invalid string', done => {
    try {
      FieldPaths.parse("");
      fail(done, "Parsing an empty string should cause an error");
    } catch (ignored) {
    }
    try {
      FieldPaths.parse(null);
      fail(done, "Parsing a null string should cause an error");
    } catch (ignored) {
    }
    done();
  });
});
