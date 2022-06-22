/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import {Error as SpineBaseError} from '../proto/spine/base/error_pb';
import {ValidationError} from '../proto/spine/validate/validation_error_pb';
import {isProtobufMessage} from './typed-message';

/**
 * The base error type of Spine Web. This error type is only used directly when a
 * more appropriate category is not defined for the offending error.
 *
 * @extends Error
*/
export class SpineError extends Error {

  /**
   * @param {!string} message the human-readable error message
   * @param {*=} cause        the reason why this error occurred
   */
  constructor(message, cause) {
    super(message);
    this.name = this.constructor.name;
    this._cause = cause;
  }

  /**
   * @return {*|undefined} The cause of this error, if available.
   */
  getCause() {
    return this._cause;
  }
}

/**
 * An error which occurs when sending off a request to Spine server endpoint fails due to the
 * connection problems.
 *
 * Can be caused by an incorrect server address, lack of network connectivity or
 * if the response is not received from the server.
 *
 * @extends SpineError
 */
export class ConnectionError extends SpineError {

  /**
   * @param {!Error} error the error caught from {@code fetch} invocation
   */
  constructor(error) {
    super(error.message, error);
  }
}

/**
 * An error which occurs when sending off a request to Spine server endpoint results
 * with a response with `5xx` status code.
 *
 * @extends SpineError
 */
export class ServerError extends SpineError {

  /**
   * @param {!Response} response the server response caused this error
   */
  constructor(response) {
    super(response.statusText, response);
  }
}

/**
 * An error which occurs when sending off a request to Spine server endpoint results
 * with a response with `4xx` status code.
 *
 * @extends SpineError
 */
export class ClientError extends SpineError {

  /**
   * @param {!string} message the human-readable error message
   * @param {*=} cause        the reason why this error occurred
   */
  constructor(message, cause) {
    super(message, cause);
  }
}

/**
 * An error which occurs when sending off a command to Spine server endpoint.
 *
 * @extends SpineError
 */
export class CommandHandlingError extends SpineError {

  /**
   * @param {!string} message the human-readable error message
   * @param {!Error|SpineError|SpineBaseError} error the reason why this error occurred
   */
  constructor(message, error) {
    super(message, error);
  }

  /**
   * Returns `true` if the command wasn't accepted by the server; returns `false`
   * if this is not guaranteed.
   *
   * A command is assumed neglected if this error is caused by the `ClientError`
   * or the `SpineBaseError`.
   *
   * @return {boolean}
   */
  assuresCommandNeglected() {
    const cause = this.getCause();
    return cause instanceof ClientError
        || isProtobufMessage(cause) && cause.constructor.typeUrl() === SpineBaseError.typeUrl();
  }
}

/**
 * An error which occurs when sending off a command to Spine server endpoint results
 * with a response indicating that a command message was rejected further processing
 * because of a validation error.
 *
 * @extends CommandHandlingError
 */
export class CommandValidationError extends CommandHandlingError {

  /**
   * @param {!string} message the human-readable error message
   * @param {!SpineBaseError} error the command validation error
   */
  constructor(message, error) {
    super(message, error);
  }

  /**
   * @return {ValidationError} command validation error
   */
  validationError() {
    return this.getCause().getValidationError();
  }
}
