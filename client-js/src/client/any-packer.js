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
import {Type, TypedMessage} from './typed-message';
import {
  Int32Value,
  Int64Value,
  StringValue
} from 'spine-web-client-proto/google/protobuf/wrappers_pb';
import {Any} from 'spine-web-client-proto/google/protobuf/any_pb';


/**
 * An packer of string, number, boolean, and message values from Protobuf `Any`.
 */
class Unpack {
  /**
   * @param {Any} any a Protobuf `Any` value to be unpacked to message or primitive
   */
  constructor(any) {
    /**
     * @type {Any}
     * @private
     */
    this._any = any;
  }

  /**
   * Unpacks a Protobuf `Any` assuming it contains a string value.
   *
   * @return {String}
   */
  asString() {
    return this.as(Type.STRING).getValue();
  }

  /**
   * Unpacks a Protobuf `Any` assuming it contains an int32 value.
   *
   * @return {Number}
   */
  asInt32() {
    return this.as(Type.INT32).getValue();
  }

  /**
   * Unpacks a Protobuf `Any` assuming it contains an unsigned int32 value.
   *
   * @return {Number}
   */
  asUInt32() {
    return this.as(Type.UINT32).getValue();
  }

  /**
   * Unpacks a Protobuf `Any` assuming it contains an int64 value.
   *
   * @return {Number}
   */
  asInt64() {
    return this.as(Type.INT64).getValue();
  }

  /**
   * Unpacks a Protobuf `Any` assuming it contains an unsigned int64 value.
   *
   * @return {Number}
   */
  asUInt64() {
    return this.as(Type.UINT64).getValue();
  }

  /**
   * Unpacks a Protobuf `Any` assuming it contains a boolean value.
   *
   * @return {boolean}
   */
  asBool() {
    return this.as(Type.BOOL).getValue();
  }

  /**
   * Unpacks a Protobuf `Any` assuming it contains a double value.
   *
   * @return {number}
   */
  asDouble() {
    return this.as(Type.DOUBLE).getValue();
  }

  /**
   * Unpacks a Protobuf `Any` assuming it contains a float value.
   *
   * @return {number}
   */
  asFloat() {
    return this.as(Type.FLOAT).getValue();
  }

  /**
   * Unpacks a Protobuf `Any` assuming it a message of the provided type.
   *
   * @param {!Type} type a type used to deserialize `Any` encompassed in this unpacker
   *
   * @return {Message} a protobuf message wrapped in `Any` deserialized using the provided type
   */
  as(type) {
    return this._any.unpack(type.class().deserializeBinary, type.url().name());
  }
}

/**
 * A packer of string, number, boolean, and message values to Protobuf `Any`.
 */
class Pack {
  /**
   * @param {*} value
   */
  constructor(value) {
    this._value = value;
  }

  /**
   * Packs the encompassed value assuming its of a string type.
   *
   * @return {Any} a new any instance with this packers value
   */
  asString() {
    return Pack._primitive(this._value.toString(), Type.STRING);
  }

  /**
   * Packs the encompassed value assuming its of an int32 type.
   *
   * @return {Any} a new any instance with this packers value
   */
  asInt32() {
    const value = Number(this._value);
    if (isNaN(value)) {
      throw new Error('The value could not be coerced to a number');
    }
    return Pack._primitive(Math.floor(value.valueOf()), Type.INT32);
  }

  /**
   * Packs the encompassed value assuming its of an unsigned int32 type.
   *
   * @return {Any} a new any instance with this packers value
   */
  asUInt32() {
    const value = Number(this._value);
    if (isNaN(value)) {
      throw new Error('The value could not be coerced to a number');
    }
    return Pack._primitive(Math.floor(value.valueOf()), Type.UINT32);
  }

  /**
   * Packs the encompassed value assuming its of an int64 type.
   *
   * @return {Any} a new any instance with this packers value
   */
  asInt64() {
    const value = Number(this._value);
    if (isNaN(value)) {
      throw new Error('The value could not be coerced to a number');
    }
    return Pack._primitive(Math.floor(value.valueOf()), Type.INT64);
  }

  /**
   * Packs the encompassed value assuming its of an unsigned int64 type.
   *
   * @return {Any} a new any instance with this packers value
   */
  asUInt64() {
    const value = Number(this._value);
    if (isNaN(value)) {
      throw new Error('The value could not be coerced to a number');
    }
    return Pack._primitive(Math.floor(value.valueOf()), Type.UINT64);
  }

  /**
   * Packs the encompassed value assuming its of a float type.
   *
   * @return {Any} a new any instance with this packers value
   */
  asFloat() {
    const value = Number(this._value);
    if (isNaN(value)) {
      throw new Error('The value could not be coerced to a number');
    }
    return Pack._primitive(value.valueOf(), Type.FLOAT);
  }

  /**
   * Packs the encompassed value assuming its of a double type.
   *
   * @return {Any} a new any instance with this packers value
   */
  asDouble() {
    const value = Number(this._value);
    if (isNaN(value)) {
      throw new Error('The value could not be coerced to a number');
    }
    return Pack._primitive(value.valueOf(), Type.DOUBLE);
  }

  /**
   * Packs the encompassed value assuming its of a boolean type.
   *
   * @return {Any} a new any instance with this packers value
   */
  asBool() {
    return Pack._primitive(!!this._value, Type.BOOL);
  }

  /**
   * @param {!Type} type a type to pack the value encompassed in this packer to
   *
   * @return {Any} a new any instance with this packers value
   */
  as(type) {
    return Pack._message(this._value, type);
  }

  /**
   * Packs a primitive (number, string, boolean) value to Protobuf `Any`.
   *
   * @param {!number|string|boolean} value a primitive value to pack
   * @param {!Type} type definition of the type to wrap the value
   *
   * @return {Any} a new `Any`
   *
   * @private
   */
  static _primitive(value, type) {
    const wrapper = type.class();
    const message = new wrapper([value]);
    return Pack._message(message, type);
  }

  /**
   * Packs a Protobuf message to Protobuf `Any`.
   *
   * @param {!Message} message a message to be packed into `Any`
   * @param {!Type} type definition of the Message type
   *
   * @return {Any} a new `Any`
   *
   * @private
   */
  static _message(message, type) {
    const typeUrl = type.url();
    const result = new Any();
    const bytes = message.serializeBinary();
    result.pack(bytes, typeUrl.name(), typeUrl.prefix());
    return result;
  }
}

/**
 * Utilities for packing messages into {@link Any} and unpacking them.
 *
 * @example
 * // Packing `TypedMessage` instance:
 * const task = new Task(message, TASK_TYPE);
 * const anyWithTask = AnyPacker.packTyped(task);
 *
 * @example
 * // Packing Protobuf messages:
 * const anyWithTask = AnyPacker.pack(message).as(TASK_TYPE);
 * const task = AnyPacker.unpack(anyWithTask).as(TASK_TYPE);
 *
 * @example
 * // Packing strings:
 * const anyWithString = AnyPacker.pack('Presents get packed too!').asString();
 * console.log(AnyPacker.unpack(anyWithString).asString());
 * // Out: Presents get packed too!
 */
export class AnyPacker {

  /**
   * Instantiation not allowed and will throw an error.
   */
  constructor() {
    throw new Error('Tried instantiating a utility class.');
  }

  /**
   * Creates a new packer for the provided `Any` instance.
   *
   * @example
   * // Unpacking messages:
   * AnyPacker.unpack(anyWithTask).as(TASK_TYPE);
   *
   * @example
   * // Unpacking primitive values:
   * AnyPacker.pack('Presents get packed too!').asString();
   *
   * @param {!Any} any a Protobuf `Any` message to unpack
   *
   * @return {Unpack} an unpacker for the provided `Any` instance
   */
  static unpack(any) {
    return new Unpack(any)
  }

  /**
   * Creates a new packer for the provided value.
   *
   * @example
   * // Packing primitive values:
   * AnyPacker.pack('Presents get packed too!').asString();
   *
   * @example
   * // Packing Protobuf messages:
   * const anyWithTask = AnyPacker.pack(message).as(TASK_TYPE);
   *
   * @param {!*} value a value of
   *
   * @return {Pack}
   */
  static pack(value) {
    return new Pack(value);
  }

  /**
   * Packs a `TypedMessage` into a Protobuf `Any`.
   *
   * @param {!TypedMessage} message a message to be packed
   *
   * @return {Any} a new Any with provided typed message inside
   */
  static packTyped(message) {
    if (!(message instanceof TypedMessage)) {
      throw new Error('Only TypedMessage instance can be packed using AnyPacker#packTyped().')
    }
    return new Pack(message.message).as(message.type);
  }
}
