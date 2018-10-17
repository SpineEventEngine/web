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

import assert from 'assert';
import sinon from 'sinon';

import {HttpEndpoint} from '../../src/client/http-endpoint';
import {HttpClient} from '../../src/client/http-client';
import {Type, TypedMessage} from '../../src/client/typed-message';
import {CreateTask} from '../../proto/test/js/spine/web/test/given/commands_pb';
import {ConnectionError,
        RequestProcessingError,
        ResponseProcessingError,
        InternalServerError} from '../../src/client/spine-web-error';
import {Duration} from '../../src/client/time-utils';

function fail(done, message) {
  return error => {
    if (message) {
      done(new Error(`Test failed. Cause: ${message}`));
    } else {
      done(new Error(`Test failed. Cause: ${error ? JSON.stringify(error) : 'not identified'}`));
    }
  };
}

/**
 * A class that represents mock HTTP Response for tests.
 */
class MockResponse {
  withStatus(status) {
    this.status = status;
    return this;
  }

  withBodyContent(bodyContent) {
    this.json = () => Promise.resolve(bodyContent);
    return this;
  }

  withMalformedBodyContent() {
    this.json = () => Promise.reject('Failed to parse from JSON');
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
Given.CONNECTION_ERROR = {message: 'Failed to fetch'};
Given.MOCK_COMMAND = new TypedMessage(
  new CreateTask(),
  Type.of(CreateTask, 'type.spine.io/spine.web.test.given.CreateTask'));

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
        assert.equal(responseParsedValue, Given.HTTP_RESPONSE.BODY_CONTENT);
        done();
      })
      .catch(fail(done, 'A message sending failed when it was expected to complete.'));
  });

  it('rejects with `ResponseProcessingError` when response body parsing fails', done => {
    const malformedResponse = Given.responseWithMalformedBody();
    httpClientBehavior.resolves(malformedResponse);

    sendCommand()
      .then(fail(done, 'A message sending was completed when it was expected to fail.'))
      .catch(error => {
        assert.ok(error instanceof ResponseProcessingError);
        assert.equal(error.reason(), malformedResponse);
        done();
      });
  });

  it('rejects with `ConnectionError` when message sending fails', done => {
    httpClientBehavior.rejects(Given.CONNECTION_ERROR);

    sendCommand()
      .then(fail(done, 'A message sending was completed when it was expected to fail.'))
      .catch(error => {
        assert.ok(error instanceof ConnectionError);
        assert.equal(error.reason(), Given.CONNECTION_ERROR);
        done()
      });
  });

  it('rejects with `RequestProcessingError` when response with status 400 received', done => {
    const responseWithClientError = Given.responseWithClientError();
    httpClientBehavior.resolves(responseWithClientError);

    sendCommand()
      .then(fail(done, 'A message sending was completed when it was expected to fail.'))
      .catch(error => {
        assert.ok(error instanceof RequestProcessingError);
        assert.equal(error.reason(), responseWithClientError);
        done()
      });
  });

  it('rejects with `InternalServerError` when response with status 500 received', done => {
    const responseWithInternalServerError = Given.responseWithServerError();
    httpClientBehavior.resolves(responseWithInternalServerError);

    sendCommand()
      .then(fail(done, 'A message sending was completed when it was expected to fail.'))
      .catch(error => {
        assert.ok(error instanceof InternalServerError);
        assert.equal(error.reason(), responseWithInternalServerError);
        done()
      });
  });
});
