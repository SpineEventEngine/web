/*
 * Copyright 2020, TeamDev. All rights reserved.
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

"use strict";

import TypeParsers from './type-parsers';
import ObjectParser from './object-parser';

import wrappers from 'google-protobuf/google/protobuf/wrappers_pb';
import struct from 'google-protobuf/google/protobuf/struct_pb';
import {Empty} from 'google-protobuf/google/protobuf/empty_pb';
import {Timestamp} from 'google-protobuf/google/protobuf/timestamp_pb';
import {Duration} from 'google-protobuf/google/protobuf/duration_pb';
import {FieldMask} from 'google-protobuf/google/protobuf/field_mask_pb';
import {Any} from 'google-protobuf/google/protobuf/any_pb';

/**
 * The parsers for standard Protobuf types.
 *
 * <p>For the details about how the parsers should work,
 * see {@link https://developers.google.com/protocol-buffers/docs/proto3#json Protobuf documentation}.
 */

class BoolValueParser extends ObjectParser {

  fromObject(object) {
    let boolValue = new wrappers.BoolValue();
    boolValue.setValue(object);
    return boolValue;
  }
}

class BytesValueParser extends ObjectParser {

  fromObject(object) {
    let bytesValue = new wrappers.BytesValue();
    bytesValue.setValue(object);
    return bytesValue;
  }
}

class DoubleValueParser extends ObjectParser {

  fromObject(object) {
    let doubleValue = new wrappers.DoubleValue();
    doubleValue.setValue(object);
    return doubleValue;
  }
}

class FloatValueParser extends ObjectParser {

  fromObject(object) {
    let floatValue = new wrappers.FloatValue();
    floatValue.setValue(object);
    return floatValue;
  }
}

class Int32ValueParser extends ObjectParser {

  fromObject(object) {
    let int32Value = new wrappers.Int32Value();
    int32Value.setValue(object);
    return int32Value;
  }
}

class Int64ValueParser extends ObjectParser {

  fromObject(object) {
    let int64Value = new wrappers.Int64Value();
    int64Value.setValue(object);
    return int64Value;
  }
}

class StringValueParser extends ObjectParser {

  fromObject(object) {
    let stringValue = new wrappers.StringValue();
    stringValue.setValue(object);
    return stringValue;
  }
}

class UInt32ValueParser extends ObjectParser {

  fromObject(object) {
    let uInt32Value = new wrappers.UInt32Value();
    uInt32Value.setValue(object);
    return uInt32Value;
  }
}

class UInt64ValueParser extends ObjectParser {

  fromObject(object) {
    let uInt64Value = new wrappers.UInt64Value();
    uInt64Value.setValue(object);
    return uInt64Value;
  }
}

class ListValueParser extends ObjectParser {

  fromObject(object) {
    let listValue = new struct.ListValue;
    object.forEach(
      function callback(currentValue, index, array) {
        let valueParser = new ValueParser();
        array[index] = valueParser.fromObject(currentValue);
      }
    );
    listValue.setValuesList(object);
    return listValue;
  }
}

class StructParser extends ObjectParser {

  fromObject(object) {
    let result = new struct.Struct();
    let values = new ValueParser();
    for (let fieldName in object) {
      result.getFieldsMap().set(fieldName, values.fromObject(object[fieldName]));
    }
    return result;
  }
}

class ValueParser extends ObjectParser {

  fromObject(object) {
    let result = new struct.Value();
    if (object === null) {
      result.setNullValue(struct.NullValue.NULL_VALUE);
    } else if (typeof object === "number") {
      result.setNumberValue(object);
    } else if (typeof object === "string") {
      result.setStringValue(object);
    } else if (typeof object === "boolean") {
      result.setBoolValue(object);
    } else if (Array.isArray(object)) {
      let parser = new ListValueParser(object);
      let listValue = parser.fromObject(object);
      result.setListValue(listValue);
    } else {
      let parser = new StructParser(object);
      let structValue = parser.fromObject(object);
      result.setStructValue(structValue);
    }
    return result;
  }
}

class EmptyParser extends ObjectParser {

  fromObject(object) {
    let emptyValue = new Empty();
    return emptyValue;
  }
}

class TimestampParser extends ObjectParser {

  fromObject(object) {
    let date = new Date(object);
    let result = new Timestamp();
    result.fromDate(date);
    return result;
  }
}

class DurationParser extends ObjectParser {

  fromObject(object) {
    object = object.substring(0, object.length - 1);
    let values = object.split(".");
    let result = new Duration();
    if (values.length === 1) {
      result.setSeconds(values[0]);
    } else if (values.length === 2) {
      result.setSeconds(values[0]);
      let nanos = values[1];
      for (let i = 0; i < 9 - nanos.length; i++) {
        nanos += "0";
      }
      let nanosNumber = parseInt(nanos, 10);
      result.setNanos(nanosNumber);
    }
    return result;
  }
}

class FieldMaskParser extends ObjectParser {

  fromObject(object) {
    let fieldMask = new FieldMask();
    fieldMask.setPathsList(object.split(","));
    return fieldMask;
  }
}

class AnyParser extends ObjectParser {

  fromObject(object) {
    const typeUrl = object["@type"];
    const isWellKnown = wellKnownParsers.has(typeUrl);
    const packedValue = isWellKnown
      ? object["value"]
      : object;
    const parser = TypeParsers.parserFor(typeUrl);
    const messageValue = parser.fromObject(packedValue);
    const bytes = messageValue.serializeBinary();
    const anyMsg = new Any();
    anyMsg.setTypeUrl(typeUrl);
    anyMsg.setValue(bytes);
    return anyMsg;
  }
}

/**
 * The map of parsers for standard Protobuf types.
 *
 * @type {Map<string, ObjectParser>}
 */
export const wellKnownParsers = new Map([
  ['type.googleapis.com/google.protobuf.BoolValue', new BoolValueParser()],
  ['type.googleapis.com/google.protobuf.BytesValue', new BytesValueParser()],
  ['type.googleapis.com/google.protobuf.DoubleValue', new DoubleValueParser()],
  ['type.googleapis.com/google.protobuf.FloatValue', new FloatValueParser()],
  ['type.googleapis.com/google.protobuf.StringValue', new StringValueParser()],
  ['type.googleapis.com/google.protobuf.Int32Value', new Int32ValueParser()],
  ['type.googleapis.com/google.protobuf.Int64Value', new Int64ValueParser()],
  ['type.googleapis.com/google.protobuf.UInt32Value', new UInt32ValueParser()],
  ['type.googleapis.com/google.protobuf.UInt64Value', new UInt64ValueParser()],
  ['type.googleapis.com/google.protobuf.Struct', new StructParser()],
  ['type.googleapis.com/google.protobuf.Value', new ValueParser()],
  ['type.googleapis.com/google.protobuf.ListValue', new ListValueParser()],
  ['type.googleapis.com/google.protobuf.Empty', new EmptyParser()],
  ['type.googleapis.com/google.protobuf.Timestamp', new TimestampParser()],
  ['type.googleapis.com/google.protobuf.Duration', new DurationParser()],
  ['type.googleapis.com/google.protobuf.FieldMask', new FieldMaskParser()],
  ['type.googleapis.com/google.protobuf.Any', new AnyParser()]
]);
