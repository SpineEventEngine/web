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

"use strict";

import base64 from 'base64-js';
import {Any} from 'spine-web-client-proto/google/protobuf/any_pb';
import {
  Int32Value,
  Int64Value,
  StringValue
} from 'spine-web-client-proto/google/protobuf/wrappers_pb';
import {WebQuery} from 'spine-web-client-proto/spine/web/web_query_pb';
import {Subscription, Topic} from 'spine-web-client-proto/spine/client/subscription_pb';
import {Command} from 'spine-web-client-proto/spine/core/command_pb';


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
    this.typeUrlPrefix = urlParts[0];
    this.typeName = urlParts[1];
    this.value = value;
  }
}

export class Type {
  /**
   * @param {Object} cls
   * @param {TypeUrl} typeUrl
   */
  constructor(cls, typeUrl) {
    this._cls = cls;
    this._typeUrl = typeUrl;
  }

  url() {
    return this._typeUrl;
  }

  class() {
    return this._cls;
  }
}

// PRIMITIVE WRAPPERS
Type.STRING = new Type(StringValue, new TypeUrl('type.googleapis.com/proto.google.protobuf.StringValue'));
Type.INT32 = new Type(Int32Value, new TypeUrl('type.googleapis.com/proto.google.protobuf.Int32Value'));
Type.INT64 = new Type(Int64Value, new TypeUrl('type.googleapis.com/proto.google.protobuf.Int64Value'));

// SPINE WEB
Type.WEB_QUERY = new Type(WebQuery, new TypeUrl('type.spine.io/spine.web.WebQuery'));

// SPINE CLIENT
Type.SUBSCRIPTION = new Type(Subscription, new TypeUrl('type.spine.io/spine.client.Subscription'));
Type.TOPIC = new Type(Topic, new TypeUrl('type.spine.io/spine.client.Topic'));

// SPINE CORE
Type.COMMAND = new Type(Command, new TypeUrl('type.spine.io/spine.core.Command'));

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
   * Converts this message into a Base64-encoded byte string.
   *
   * @return the string representing this message
   */
  toBase64() {
    const bytes = this.message.serializeBinary();
    return base64.fromByteArray(bytes);
  }
}
