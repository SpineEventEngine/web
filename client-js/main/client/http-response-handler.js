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

import {ClientError, ConnectionError, ServerError, SpineError} from "./errors";

/**
 * Receives the HTTP response, and turns it into a JS object.
 *
 * Handles the response failures as well, in which case a corresponding error object
 * is returned via a `Promise`.
 *
 * Only `2xx` response codes count as successful. All other response
 * codes are considered erroneous.
 *
 * By default, expects the input to be a JSON string. Users may choose to customize the behavior
 * by extending this type, and supplying the custom implementation via {@link ClientOptions}.
 */
export class HttpResponseHandler {

    /**
     * Retrieves the JS object by transforming the contents
     * of the given HTTP response if it was successful,
     * rejects with a respective error otherwise.
     *
     * @param {!Response} response an HTTP request response
     * @return {Promise<Object|SpineError>} a promise of a successful server response data,
     *                                      rejected if the client response is not `2xx`,
     *                                      or if the transformation-to-object fails
     *                                      for the response contents.
     * @see _parse
     */
    handle(response) {
        const statusCode = response.status;
        if (HttpResponseHandler._isSuccessfulResponse(statusCode)) {
            return this._parse(response);
        } else if (HttpResponseHandler._isClientErrorResponse(statusCode)) {
            return Promise.reject(new ClientError(response.statusText, response));
        } else if (HttpResponseHandler._isServerErrorResponse(statusCode)) {
            return Promise.reject(new ServerError(response));
        }
    }

    /**
     * Transforms the response into JS object by parsing the response contents.
     *
     * This implementation expects the response to contain JSON data.
     *
     * @param response an HTTP response
     * @return {Promise<Object|SpineError>} a promise of JS object,
     *                                      or a rejection with the corresponding `SpineError`
     * @protected
     */
    _parse(response) {
        return response.json()
            .then(json => Promise.resolve(json))
            .catch(error =>
                Promise.reject(new SpineError('Failed to parse response JSON', error))
            );
    }

    /**
     * Obtains the error caught from and erroneous HTTP request, and returns
     * a rejected promise with a given error wrapped into {@link ConnectionError}.
     *
     * This handling method differs from others, since it is designed to handle the issues
     * which were caused by an inability to send the HTTP request itself — so in this case
     * there is no HTTP response. Note, that {@link handle} is designed
     * to process the HTTP response, including erroneous responses.
     *
     * @param {!Error} error              an error which occurred upon sending an HTTP request
     * @return {Promise<ConnectionError>} a rejected promise with a `ConnectionError`
     */
    onConnectionError(error) {
        return Promise.reject(new ConnectionError(error));
    }

    /**
     * @param {!number} statusCode an HTTP request response status code
     * @return {boolean} `true` if the response status code is from 200 to 299,
     *                   `false` otherwise
     * @protected
     */
    static _isSuccessfulResponse(statusCode) {
        return 200 <= statusCode && statusCode < 300;
    }

    /**
     * @param {!number} statusCode an HTTP request response status code
     * @return {boolean} `true` if the response status code is from 400 to 499,
     *                   `false` otherwise
     * @protected
     */
    static _isClientErrorResponse(statusCode) {
        return 400 <= statusCode && statusCode < 500;
    }

    /**
     * @param {!number} statusCode an HTTP request response status code
     * @return {boolean} `true` if the response status code is from 500,
     *                   `false` otherwise
     * @protected
     */
    static _isServerErrorResponse(statusCode) {
        return 500 <= statusCode;
    }
}