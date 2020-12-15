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

import {Message} from 'google-protobuf';
import base64 from 'base64-js';
import {
  BoolValue,
  DoubleValue,
  FloatValue,
  Int32Value,
  Int64Value,
  StringValue,
  UInt32Value,
  UInt64Value,
} from '../proto/google/protobuf/wrappers_pb';
import {convertDateToTimestamp} from './time-utils';
import KnownTypes from './known-types';

/**
 * Checks if the object extends {@link Message}.
 *
 * <p>The implementation doesn't use `instanceof` check and check on prototypes
 * since they may fail if different versions of the file are used at the same time
 * (e.g. bundled and the original one).
 *
 * @param object the object to check
 */
export function isProtobufMessage(object) {
  return typeof object.constructor.typeUrl === 'function';
}

/**
 * A URL of a Protobuf type.
 *
 * Consists of the two parts separated with a slash. The first part is
 * the type URL prefix (for example, `type.googleapis.com`).
 * The second part is a fully-qualified Protobuf type name.
 *
 * @template <T>
 */
export class TypeUrl {

  /**
   * Creates a new instance of TypeUrl from the given string value.
   *
   * The value should be a valid type URL of format:
   * (typeUrlPrefix)/(typeName)
   *
   * @param {!string} value the type URL value
   */
  constructor(value) {
    const urlParts = value.split('/');
    this._value = value;
    this._prefix = urlParts[0];
    this._name = urlParts[1];
  }

  /**
   * @return {string} part of the type URL before `/` specifying a namespace
   */
  prefix() {
    return this._prefix;
  }

  /**
   * @return {string} part of the type URL after `/` specifying the type name
   */
  name() {
    return this._name;
  }

  /**
   * @return {!string} full type URL value formatted as `<prefix>/<name>` string
   */
  value() {
    return this._value;
  }
}

/**
 * A type of the Protobuf message represented by its JavaScript class and type URL.
 *
 * @template <T>
 */
export class Type {

  /**
   * @param {!Class<T>} cls a class of the `Message` type is for
   * @param {!TypeUrl<T>} typeUrl a type URL of the `Message` type is for
   */
  constructor(cls, typeUrl) {
    this._cls = cls;
    this._typeUrl = typeUrl;
  }

  /**
   * @return {!TypeUrl<T>} a type URL of a defined type
   */
  url() {
    return this._typeUrl;
  }

  /**
   * @return {!Class<T>} a JS class of a defined type
   */
  class() {
    return this._cls;
  }

  /**
   * A static factory for creating Type instances from `Message` class and type URL.
   *
   * @param {!Class<T>} cls a class of the `Message` type is for
   * @param {!string|TypeUrl<T>} typeUrl a type URL of the `Message` type is for
   *
   * @return {Type<T>} new Type instance
   */
  static of(cls, typeUrl) {
    if (!(typeUrl instanceof TypeUrl)) {
      typeUrl = new TypeUrl(typeUrl);
    }
    return new Type(cls, typeUrl);
  }

  /**
   * Creates a new `Type` for the passed `Message` class.
   *
   * @param cls {!Class<T>} cls a class of the `Message`
   */
  static forClass(cls) {
    const typeUrl = KnownTypes.typeUrlFor(cls);
    return this.of(cls, typeUrl);
  }

  /**
   * Creates a new `Type` for the passed `Message`.
   *
   * @param {!Message} message a Protobuf message
   */
  static forMessage(message) {
    return this.forClass(message.constructor);
  }
}

// PRIMITIVE WRAPPERS
Type.STRING = Type.forClass(StringValue);
Type.INT32 = Type.forClass(Int32Value);
Type.UINT32 = Type.forClass(UInt32Value);
Type.INT64 = Type.forClass(Int64Value);
Type.UINT64 = Type.forClass(UInt64Value);
Type.BOOL = Type.forClass(BoolValue);
Type.DOUBLE = Type.forClass(DoubleValue);
Type.FLOAT = Type.forClass(FloatValue);

/**
 * A Protobuf message with a {@link TypeUrl}.
 *
 * The type URL specifies the type of the associated message.
 *
 * @template <T>
 */
export class TypedMessage {

  /**
   * Creates a new instance of TypedMessage from the given Protobuf message and
   * type URL.
   *
   * @param {!Message} message a Protobuf message
   * @param {!Type<T>} type a Protobuf type of the message
   */
  constructor(message, type) {
    this.message = message;
    this.type = type;
  }

  /**
   * Creates a new instance of TypedMessage from the given Protobuf message.
   *
   * @param {!Message} message a Protobuf message
   * @returns {!TypedMessage} the created typed message
   */
  static of(message) {
    const type = Type.forMessage(message);
    return new TypedMessage(message, type);
  }

  /**
   * Converts this message into a Base64-encoded byte string.
   *
   * @return the string representing this message
   */
  toBase64() {
    const bytes = this.message.serializeBinary();
    return base64.fromByteArray(bytes);
  }

  /**
   * Creates a new `TypedMessage` wrapping a string.
   *
   * @param {!String} value a string value for `TypedMessage`
   * @return {TypedMessage<StringValue>} a new `TypedMessage` instance with provided value
   */
  static string(value) {
    const message = new StringValue([value.toString()]);
    return TypedMessage.of(message);
  }

  /**
   * Creates a new `TypedMessage` with a 32-bit integer value.
   *
   * The number gets floored if the provided value contains a floating point.
   *
   * @param {!number|Number} value a number value for `TypedMessage`
   * @return {TypedMessage<Int32Value>} a new `TypedMessage` instance with provided value
   */
  static int32(value) {
    const message = new Int32Value([Math.floor(value.valueOf())]);
    return TypedMessage.of(message);
  }

  /**
   * Creates a new `TypedMessage` with an unsigned 32-bit integer value.
   *
   * The number gets floored if the provided value contains a floating point.
   *
   * @param {!number|Number} value a number value for `TypedMessage`
   * @return {TypedMessage<UInt32Value>} a new `TypedMessage` instance with provided value
   */
  static uint32(value) {
    const message = new UInt32Value([Math.floor(value.valueOf())]);
    return TypedMessage.of(message);
  }

  /**
   * Creates a new `TypedMessage` with a 64-bit integer value.
   *
   * The number gets floored if the provided value contains a floating point.
   *
   * @param {!number|Number} value a number value for `TypedMessage`
   * @return {TypedMessage<Int64Value>} a new `TypedMessage` instance with provided value
   */
  static int64(value) {
    const message = new Int64Value([Math.floor(value.valueOf())]);
    return TypedMessage.of(message);
  }

  /**
   * Creates a new `TypedMessage` with an unsigned 64-bit integer value.
   *
   * The number gets floored if the provided value contains a floating point.
   *
   * @param {!number|Number} value a number value for `TypedMessage`
   * @return {TypedMessage<UInt64Value>} a new `TypedMessage` instance with provided value
   */
  static uint64(value) {
    const message = new UInt64Value([Math.floor(value.valueOf())]);
    return TypedMessage.of(message);
  }

  /**
   * Creates a new `TypedMessage` with a float value.
   *
   * @param {!number|Number} value a number value for `TypedMessage`
   * @return {TypedMessage<FloatValue>} a new `TypedMessage` instance with provided value
   */
  static float(value) {
    const message = new FloatValue([value.valueOf()]);
    return TypedMessage.of(message);
  }

  /**
   * Creates a new `TypedMessage` with a double value.
   *
   * @param {!number|Number} value a number value for `TypedMessage`
   * @return {TypedMessage<DoubleValue>} a new `TypedMessage` instance with provided value
   */
  static double(value) {
    const message = new DoubleValue([value.valueOf()]);
    return TypedMessage.of(message);
  }

  /**
   * Creates a new `TypedMessage` with a boolean value.
   *
   * @param {!boolean|Boolean} value `true` or `false` value for the `TypedMessage`
   * @return {TypedMessage<BoolValue>} a new `TypedMessage` instance with provided value
   */
  static bool(value) {
    const message = new BoolValue([value]);
    return TypedMessage.of(message);
  }

  /**
   * Creates a new `TypedMessage` with a timestamp value composed from the given JavaScript date.
   *
   * @param {!Date} date a JavaScript `Date` value
   * @return {TypedMessage<Timestamp>} a new `TypedMessage` instance with provided value
   */
  static timestamp(date) {
    const message = convertDateToTimestamp(date);
    return TypedMessage.of(message);
  }
}
