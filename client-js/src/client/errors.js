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

/**
* An error which occurs when sending off a request to Spine server endpoint.
*/
export class SpineError extends Error {

  /**
   * @param {!string} message the human-readable error message
   * @param {*=} cause        the reason why this error occurred
   */
  constructor(message, cause) {
    super(message);
    this.name = this.constructor.name;
    this.cause = cause;
  }

  getCause() {
    return this.cause;
  }
}

/**
 * An error which occurs when sending off a request to Spine server endpoint fails due to the
 * connection problems.
 *
 * It may be caused by an incorrect server address, lack of network connectivity and situations
 * in which the response to the sent request is not received.
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
 * Indicates an unhandled exception was thrown upon the request processing.
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
 * An error indicating an invalid client behaviour.
 *
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
 * An error which occurs when sending off a command to Spine server endpoint results
 * with a response which indicates that a command message was rejected further processing
 * (e.g. because of a validation error).
 *
 * @extends SpineError
 */
export class CommandHandlingError extends SpineError {

  /**
   * @param {spine.base.Error} error the technical error occurred upon receiving the request and
   *                                 no further processing would occur.
   */
  constructor(error) {
    super(error.message, error);
  }

  /**
   * Returns the type of {@code spine.base.Error}.
   *
   * @return {string}
   */
  type() {
    return this.getCause().type;
  }

  /**
   * Returns the code of {@code spine.base.Error}.
   *
   * @return {number}
   */
  code() {
    return this.getCause().code;
  }

  /**
   * Returns an optional validation error object error indicating that a
   * message sent to the server did not pass validation.
   *
   * @return {?spine.validate.ValidationError}
   */
  validationError() {
    return this.getCause().validationError;
  }
}

/**
 * Typedef representing the type union of `client-js` module errors.
 *
 * @typedef {Object} Errors
 *
 * @property {SpineError} SpineError
 * @property {ConnectionError} ConnectionError
 * @property {ServerError} ServerError
 * @property {ClientError} ClientError
 * @property {CommandHandlingError} CommandHandlingError
 */

/**
 * The object which represents the type union of `client-js` module errors.
 *
 * @type {Errors}
 */
export const Errors = {
   SpineError,
   ConnectionError,
   ServerError,
   ClientError,
   CommandHandlingError,
};
