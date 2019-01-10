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

import assert from 'assert';

import TypeParsers from '../../../src/client/parser/type-parsers';

import {Any} from 'spine-web-client-proto/google/protobuf/any_pb';
import {StringValue} from 'spine-web-client-proto/google/protobuf/wrappers_pb';
import {StringChange} from 'spine-web-client-proto/spine/change/change_pb';

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
    assert.equal(parsedString.getValue(), stringToParse);
  });

  it('parses any with a well known message inside', () => {
    const anyObject = {
      '@type': StringValue.typeUrl(),
      'value': 'Packed string'
    };
    const parser = TypeParsers.parserFor(Any.typeUrl());
    const parsedAny = parser.fromObject(anyObject);
    const packedString = StringValue.deserializeBinary(parsedAny.getValue());
    assert.equal(parsedAny.getTypeUrl(), StringValue.typeUrl());
    assert.equal(packedString.getValue(), anyObject.value);
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
    assert.equal(parsedAny.getTypeUrl(), StringChange.typeUrl());
    assert.equal(packagedMessage.getPreviousValue(), anyObject.previousValue);
    assert.equal(packagedMessage.getNewValue(), anyObject.newValue);
  });
});
