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


class Unpack {
  /**
   * @param {Any} any
   */
  constructor(any) {
    /**
     * @type {Any}
     * @private
     */
    this._any = any;
  }

  /**
   * @return {String}
   */
  asString() {
    return this.as(Type.STRING).getValue();
  }

  /**
   * @return {Number}
   */
  asInt32() {
    return this.as(Type.INT32).getValue();
  }

  /**
   * @return {Number}
   */
  asInt64() {
    return this.as(TYPE.INT64).getValue();
  }

  /**
   * @param {Type} type
   * @return {Message}
   */
  as(type) {
    return this._any.unpack(type.class().deserializeBinary, type.url().typeName);
  }
}

class Pack {
  /**
   * @param {*} value
   */
  constructor(value) {
    this._value = value;
  }

  /**
   * @return {String}
   */
  asString() {
    return Pack._primitive(this._value, Type.STRING);
  }

  /**
   * @return {String}
   */
  asInt32() {
    return Pack._primitive(this._value, Type.INT32);
  }

  /**
   * @return {String}
   */
  asInt64() {
    return Pack._primitive(this._value, Type.INT64);
  }

  /**
   * @param {Type} type
   * @return {Any}
   */
  as(type) {
    return Pack._message(this._value, type);
  }

  /**
   * @param {Type} type
   * @private
   */
  _wrapPrimitive(type) {
    const wrapper = type.class();
    return wrapper([this._value]);
  }

  static _primitive(value, type) {
    const wrapper = type.class();
    const message = wrapper([value]);
    return Pack._message(message, type);
  }

  static _message(message, type) {
    const typeUrl = type.url();
    const result = new Any();
    const bytes = message.serializeBinary();
    result.pack(bytes, typeUrl.typeName, typeUrl.typeUrlPrefix);
    return result;
  }
}


export class AnyPacker {
  static string(value) {
    const result = new StringValue();
    result.setValue(value);
    return result;
  }

  /**
   * @param {!Any} any
   * @return {Unpack}
   */
  static unpack(any) {
    return new Unpack(any)
  }

  /**
   * @param {!Message|string|number} message
   * @return {Pack}
   */
  static pack(message) {
    return new Pack(message);
  }

  /**
   * @param {!TypedMessage} value
   * @return {Any}
   */
  static packTyped(value) {
    return new Pack(value.message).as(value.type);
  }
}
