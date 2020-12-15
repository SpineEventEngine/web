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
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import TypeParsers from './parser/type-parsers';
import KnownTypes from './known-types';

/**
 * A utility which converts the JS object to its Protobuf counterpart.
 */
export default class ObjectToProto {

  constructor() {
    throw new Error('ObjectToProto is not supposed to be instantiated.');
  }

  /**
   * Converts the object to the corresponding Protobuf message.
   *
   * The input object is supposed to be a Protobuf message representation, i.e. all of its attributes should
   * correspond to the fields of the specified message type.
   *
   * @param {!Object} object an object to convert
   * @param {!string} typeUrl a type URL of the corresponding Protobuf message
   */
  static convert(object, typeUrl) {
    if (!KnownTypes.hasType(typeUrl)) {
      throw new Error(`Unable to convert object of unknown type ${typeUrl}`);
    }
    const parser = TypeParsers.parserFor(typeUrl);
    const proto = parser.fromObject(object);
    return proto;
  }

  /**
   * Convert the given observable of objects to the observable of the
   * corresponding Protobuf messages.
   *
   * @param {!Observable<Object>} observable an observable to convert
   * @param {!string} typeUrl a type URL of the corresponding Protobuf message
   * @return {Observable<Message>} an observable of converted Protobuf messages
   */
  static map(observable, typeUrl) {
    return observable.pipe(
      map(object => ObjectToProto.convert(object, typeUrl))
    );
  }
}
