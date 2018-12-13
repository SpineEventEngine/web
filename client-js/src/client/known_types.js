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

/**
 * All Protobuf types known to the application.
 *
 * <p>This class serves as the registry of known types generated by ProtoJsPlugin.
 */
export default class KnownTypes {

  constructor() {
    throw new Error('KnownTypes is not supposed to be instantiated.')
  }

  /**
   * Registers the subset of known types.
   *
   * <p>Types are skipped if some of them were already registered.
   *
   * @param knownTypesSubset {!Map}
   * @public
   */
  static with(knownTypesSubset) {
    for (let [typeUrl, messageClass] of knownTypesSubset.entries()) {
      if (!types.has(typeUrl)) {
        types.set(typeUrl, messageClass);
      }
    }
  }

  /**
   * Finds the type URL for the Protobuf message in the known types.
   *
   * @param {!Class} messageClass the class of a Protobuf message
   * @returns {!string} the type URL
   * @throws {Error} if the message class in unknown
   * @public
   */
  static typeUrlFor(messageClass) {
    //TODO:2018-12-10:dmytro.grankin: swap keys and values to achieve O(1)
    for (let [typeUrl, type] of types.entries()) {
      if (type === messageClass) {
        return typeUrl;
      }
    }
    //TODO:2018-12-13:dmytro.grankin: test lookup of types that are registered twice
    throw new Error('Cannot find the TypeUrl for a message class.');
  }
}

/**
 * The map of all Protobuf types known to the application.
 *
 * <p>It is intended to be a static variable, but ES6 doesn't provide an easy way to do it.
 *
 * @type {Map<String, Class>}
 * @private
 */
const types = new Map();
