/**
* An error which occurs when sending off a request to Spine server endpoint.
*
* @abstract
*/
export class SpineWebError {
  /**
   * @param {Object} reason the reason why this error occurred
   */
  constructor(reason) {
    this._reason = reason;
  }

  /**
   * @return {Object} the reason of the error
   */
  reason() {
    return this._reason;
  }
}

/**
 * An error which occurs when sending off a request to Spine server endpoint fails due to connection problems.
 * Summarizes situations in which the response to the sent request is not received.
 *
 * The cause may be an incorrect server address or lack of network connectivity.
 *
 * @extends SpineWebError
 */
export class ConnectionError extends SpineWebError {
  /**
   * @param {Object} error the reason why this error occurred
   */
  constructor(error) {
    super(error)
  }
}

/**
 * An error which occurs when sending off a request to Spine server endpoint results with a response
 * with `5xx` status code.
 *
 * Indicates an unhandled exception was thrown upon the request processing.
 *
 * @extends SpineWebError
 */
export class InternalServerError extends SpineWebError {
  /**
   * @param {Object} response the reason why this error occurred
   */
  constructor(response) {
    super(response)
  }

  getCode() {
    return this.reason().status;
  }
}

/**
 * An abstract error indicating an invalid client behaviour.
 *
 * @extends SpineWebError
 * @abstract
 */
export class ClientError extends SpineWebError {
  /**
   * @param {Object} reason the reason why this error occurred
   */
  constructor(reason) {
    super(reason)
  }
}

/**
 * An error which occurs when sending off a request to Spine server endpoint results with a response
 * with `4xx` status code.
 *
 * @extends ClientError
 */
export class RequestProcessingError extends ClientError {
  /**
   * @param {Object} response the reason why this error occurred
   */
  constructor(response) {
    super(response)
  }

  getCode() {
    return this.reason().status;
  }
}

/**
 * An error which occurs when sending off a command to Spine server endpoint results with a response
 * containing {@code spine.base.Ack} with error status. It means that a command message was rejected further
 * processing (e.g. because of a validation error).
 *
 * @extends ClientError
 */
export class CommandProcessingError extends ClientError {
  /**
   * @param {spine.base.Error} error the technical error occurred upon receiving the request and no further
   *                                 processing would occur.
   */
  constructor(error) {
    super(error)
  }

  getType() {
    return this.reason().type;
  }

  getCode() {
    return this.reason().code;
  }
}
