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

"use strict";

import {Message} from 'google-protobuf';

/**
 * Parses a plain Javascript object to a Protobuf message.
 *
 * The class is abstract and should be implemented for every message type.
 */
export default class ObjectParser {

  /**
   * Creates a new instance.
   */
  constructor() {
    if (this.constructor === ObjectParser) {
      throw new Error('Cannot instantiate abstract ObjectParser class.');
    }
  }

  /**
   * Converts an object to a message.
   *
   * @abstract
   * @param {!Object} object the object representing a Protobuf message
   * @return {!Message} the parsed Protobuf message
   */
  fromObject(object) {
    throw new Error('The method is abstract and should be implemented by a subclass');
  }

  /**
   * Checks if the parser extends {@link ObjectParser}.
   *
   * <p>The implementation doesn't use `instanceof` check and check on prototypes
   * since they may fail if different versions of the file are used at the same time
   * (e.g. bundled and the original one).
   *
   * @param object the object to check
   */
  static isParser(object) {
    const abstractMethod = object.fromObject;
    const methodDefined = typeof abstractMethod === 'function';
    return methodDefined;
  }
}
