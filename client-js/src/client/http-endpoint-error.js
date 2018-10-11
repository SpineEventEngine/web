/**
* An error which occurred when sending off a request to Spine server endpoint.
*/
export class EndpointError {
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

export class ConnectionError extends EndpointError {
    /**
     * @param {Object} error the reason why this error occurred
     */
    constructor(error) {
        super(error)
    }
}

export class InternalServerError extends EndpointError {
    /**
     * @param {Object} response the reason why this error occurred
     */
    constructor(response) {
        super(response)
    }
}

export class ClientError extends EndpointError {
    /**
     * @param {Object} response the reason why this error occurred
     */
    constructor(response) {
        super(response)
    }
}

export class CommandValidationError extends EndpointError {
    /**
     * @param {Object} response the reason why this error occurred
     */
    constructor(response) {
        super(response)
    }
}
