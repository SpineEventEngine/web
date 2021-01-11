/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import assert from 'assert';
import sinon from 'sinon';

import {HttpEndpoint} from '@lib/client/http-endpoint';
import {HttpClient} from '@lib/client/http-client';
import {TypedMessage} from '@lib/client/typed-message';
import {CreateTask} from '@testProto/spine/test/js/commands_pb';
import {ClientError, ConnectionError, ServerError, SpineError} from '@lib/client/errors';
import {Duration} from '@lib/client/time-utils';
import {fail} from './test-helpers';

const MOCK_RESPONSE_STATUS_TEXT = 'Status text';

/**
 * A class that represents mock HTTP Response for tests.
 */
class MockResponse {
  withStatus(status) {
    this.status = status;
    this.statusText = MOCK_RESPONSE_STATUS_TEXT;
    return this;
  }

  withBodyContent(bodyContent) {
    this.json = () => Promise.resolve(bodyContent);
    return this;
  }

  withMalformedBodyContent() {
    this.json = () => Promise.reject(new Error('Failed to parse JSON'));
    return this;
  }
}

class Given {
  constructor() {
    throw new Error('A utility Given class cannot be instantiated.');
  }

  static httpClient() {
    return new HttpClient(Given.FAKE_ENDPOINT_URL);
  }

  static response() {
    return this._mockResponse()
        .withStatus(Given.HTTP_RESPONSE.STATUS.OK)
        .withBodyContent(Given.HTTP_RESPONSE.BODY_CONTENT);
  }

  static responseWithClientError() {
    return this._mockResponse()
        .withStatus(Given.HTTP_RESPONSE.STATUS.CLIENT_ERROR);
  }

  static responseWithServerError() {
    return this._mockResponse()
        .withStatus(Given.HTTP_RESPONSE.STATUS.SERVER_ERROR);
  }

  static responseWithMalformedBody() {
    return this._mockResponse()
        .withStatus(Given.HTTP_RESPONSE.STATUS.OK)
        .withMalformedBodyContent();
  }

  static _mockResponse() {
    return new MockResponse();
  }
}

Given.FAKE_ENDPOINT_URL = 'https://fake-endpoint.url';
Given.CONNECTION_ERROR = new Error('Failed to fetch');
Given.MOCK_COMMAND = TypedMessage.of(new CreateTask);

Given.HTTP_RESPONSE = {
  STATUS: {
    OK: 200,
    CLIENT_ERROR: 400,
    SERVER_ERROR: 500
  },
  BODY_CONTENT: 'Command acknowledged'
};

const httpClient = Given.httpClient();
const httpEndpoint = new HttpEndpoint(httpClient);

describe('HttpEndpoint.command', function () {
  const timeoutDuration = new Duration({seconds: 5});
  this.timeout(timeoutDuration.inMs());

  let httpClientBehavior;

  function sendCommand() {
    return httpEndpoint.command(Given.MOCK_COMMAND);
  }

  beforeEach(() => {
    httpClientBehavior = sinon.stub(httpClient, 'postMessage');
  });

  afterEach(() => {
    httpClientBehavior.restore();
  });

  it('resolves with value parsed from response body', done => {
    httpClientBehavior.resolves(Given.response());

    sendCommand()
        .then(responseParsedValue => {
          assert.strictEqual(responseParsedValue, Given.HTTP_RESPONSE.BODY_CONTENT);
          done();
        })
        .catch(fail(done, 'A message sending failed when it was expected to complete.'));
  });

  it('rejects with `SpineError` when response body parsing fails', done => {
    const malformedResponse = Given.responseWithMalformedBody();
    httpClientBehavior.resolves(malformedResponse);

    sendCommand()
        .then(fail(done, 'A message sending was completed when it was expected to fail.'))
        .catch(error => {
          assert.ok(error instanceof SpineError);
          assert.ok(error.getCause() instanceof Error);
          assert.strictEqual(error.message, 'Failed to parse response JSON');
          done();
        });
  });

  it('rejects with `ConnectionError` when message sending fails', done => {
    httpClientBehavior.rejects(Given.CONNECTION_ERROR);

    sendCommand()
        .then(fail(done, 'A message sending was completed when it was expected to fail.'))
        .catch(error => {
          assert.ok(error instanceof ConnectionError);
          assert.ok(error.getCause() instanceof Error);
          assert.strictEqual(error.message, Given.CONNECTION_ERROR.message);
          done();
        });
  });

  it('rejects with `ClientError` when response with status 400 received', done => {
    const responseWithClientError = Given.responseWithClientError();
    httpClientBehavior.resolves(responseWithClientError);

    sendCommand()
        .then(fail(done, 'A message sending was completed when it was expected to fail.'))
        .catch(error => {
          assert.ok(error instanceof ClientError);
          assert.strictEqual(error.message, MOCK_RESPONSE_STATUS_TEXT);
          assert.strictEqual(error.getCause(), responseWithClientError);
          done();
        });
  });

  it('rejects with `ServerError` when response with status 500 received', done => {
    const responseWithServerError = Given.responseWithServerError();
    httpClientBehavior.resolves(responseWithServerError);

    sendCommand()
        .then(fail(done, 'A message sending was completed when it was expected to fail.'))
        .catch(error => {
          assert.ok(error instanceof ServerError);
          assert.strictEqual(error.message, MOCK_RESPONSE_STATUS_TEXT);
          assert.strictEqual(error.getCause(), responseWithServerError);
          done();
        });
  });
});
