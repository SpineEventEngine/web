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

import TypeParsers from '@lib/client/parser/type-parsers';

import {Any} from '@proto/google/protobuf/any_pb';
import {StringValue} from '@proto/google/protobuf/wrappers_pb';
import {Struct} from '@proto/google/protobuf/struct_pb';
import {StringChange} from '@proto/spine/change/change_pb';

import {registerProtobufTypes} from './../test-helpers';

describe('TypeParsers', () => {

  registerProtobufTypes();

  it('autoregisters parsers for standard Protobuf types', () => {
    const parser = TypeParsers.parserFor(Any.typeUrl());
    assert.ok(parser);
  });

  it('requires a parser to extend ObjectParser', () => {
    assert.throws(
      () => TypeParsers.register({}, Any.typeUrl())
    );
  });

  it('parses a string', () => {
    const stringToParse = "Protobuf String";
    const parser = TypeParsers.parserFor(StringValue.typeUrl());
    const parsedString = parser.fromObject(stringToParse);
    assert.strictEqual(parsedString.getValue(), stringToParse);
  });

  it('parses any with a well known message inside', () => {
    const anyObject = {
      '@type': StringValue.typeUrl(),
      'value': 'Packed string'
    };
    const parser = TypeParsers.parserFor(Any.typeUrl());
    const parsedAny = parser.fromObject(anyObject);
    const packedString = StringValue.deserializeBinary(parsedAny.getValue());
    assert.strictEqual(parsedAny.getTypeUrl(), StringValue.typeUrl());
    assert.strictEqual(packedString.getValue(), anyObject.value);
  });

  it('parses any with a custom message inside', () => {
    const anyObject = {
      '@type': StringChange.typeUrl(),
      'previousValue': 'prev value',
      'newValue': 'new value'
    };
    const parser = TypeParsers.parserFor(Any.typeUrl());
    const parsedAny = parser.fromObject(anyObject);
    const packagedMessage = StringChange.deserializeBinary(parsedAny.getValue());
    assert.strictEqual(parsedAny.getTypeUrl(), StringChange.typeUrl());
    assert.strictEqual(packagedMessage.getPreviousValue(), anyObject.previousValue);
    assert.strictEqual(packagedMessage.getNewValue(), anyObject.newValue);
  });

  it('parses Structs and Values', () => {
    const struct = {
      'foo': 42,
      'bar': {
        'baz': 'my-string'
      },
      'newValue': 'new value',
      'anArray': ['eins', 'zwei', 'drei']
    };
    const parser = TypeParsers.parserFor(Struct.typeUrl());
    const parsed = parser.fromObject(struct);
    const map = parsed.getFieldsMap();
    assert.strictEqual(
        map.get('foo').getNumberValue(),
        struct['foo']
    );
    assert.strictEqual(
        map.get('bar').getStructValue().getFieldsMap().get('baz').getStringValue(),
        struct['bar']['baz']
    );
    assert.strictEqual(
        map.get('newValue').getStringValue(),
        struct['newValue']
    );
    assert.strictEqual(
        map.get('anArray').getListValue().getValuesList(),
        struct['anArray']
    );
  });
});
