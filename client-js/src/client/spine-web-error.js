/**
* An error which occurs when sending off a request to Spine server endpoint.
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
 * An error which occurs when sending off a request to Spine server endpoint results in a response
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
}

/**
 * An error which occurs when sending off a request to Spine server endpoint results in a response
 * with `4xx` status code.
 *
 * @extends SpineWebError
 */
export class ClientError extends SpineWebError {
    /**
     * @param {Object} response the reason why this error occurred
     */
    constructor(response) {
        super(response)
    }
}

/**
 * An error which occurs when sending off a command to Spine server endpoint results in a response
 * containing {@code spine.base.Ack} with error status. That means that a message was rejected further
 * processing (e.g. because of a validation error).
 *
 * @extends SpineWebError
 */
export class CommandValidationError extends SpineWebError {
    /**
     * @param {spine.base.Error} error the technical error occurred upon receiving the request and no further
     *                                 processing would occur.
     */
    constructor(error) {
        super(error)
    }
}
