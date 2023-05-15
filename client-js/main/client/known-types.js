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

"use strict";

/**
 * The map of all Protobuf types known to the application.
 *
 * <p>It is intended to be a static variable, but ES6 doesn't provide an easy way to do it.
 *
 * @type {Map<String, Class>}
 * @private
 */
const types = new Map();

/**
 * All Protobuf types known to the application.
 *
 * <p>This class serves as the registry of types generated by Protobuf complier.
 */
export default class KnownTypes {

  constructor() {
    throw new Error('KnownTypes is not supposed to be instantiated.');
  }

  /**
   * Obtains the type URL for the Protobuf type.
   *
   * @param {!Class} messageClass the class of a Protobuf message or enum
   * @return {!string} the type URL
   * @public
   */
  static typeUrlFor(messageClass) {
    return messageClass.typeUrl();
  }

  /**
   * Obtains JS class for the given Protobuf type URL.
   *
   * @param {!string} typeUrl the type URL
   * @return {!Class} class of this Protobuf type
   * @public
   */
  static classFor(typeUrl) {
    const cls = types.get(typeUrl);
    if (cls === null || cls === undefined) {
      throw new Error(`Class for type URL '${typeUrl}' is not found.`);
    }
    return cls;
  }

  /**
   * Registers the type as a known type.
   *
   * @param {!Class} type the class of a Protobuf message or enum
   * @param {!string} typeUrl the URL of the type
   */
  static register(type, typeUrl) {
    if (!KnownTypes.hasType(typeUrl)) {
      types.set(typeUrl, type);
    }
  }

  /**
   * Tells whether the specified type URL is present among known types.
   *
   * @param {!string} typeUrl the type URL to check
   */
  static hasType(typeUrl) {
    return types.has(typeUrl);
  }

  /**
   * Removes all the types.
   *
   * The method is purposed for the testing.
   */
  static clear() {
    types.clear();
  }
}
