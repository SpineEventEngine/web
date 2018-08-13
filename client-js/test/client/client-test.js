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

// noinspection NodeJsCodingAssistanceForCoreModules
import assert from 'assert';

import {ActorRequestFactory} from '../../src/client/actor-request-factory';
import {FirebaseClient} from '../../src/client/firebase-client';
import {devFirebaseApp as firebase} from './test-firebase-app';
import {TypedMessage, TypeUrl} from '../../src/client/typed-message';

import {CreateTask} from '../../proto/test/js/spine/web/test/commands_pb';
import {TaskId} from '../../proto/test/js/spine/web/test/task_pb';
import {HttpClient} from '../../src/client/http-client';
import {HttpEndpoint} from '../../src/client/http-endpoint';
import {BackendClient} from '../../src/client/backend-client';

const MILLISECONDS = 1;
const SECONDS = 1000 * MILLISECONDS;
const MINUTES = 60 * SECONDS;

function creteTaskCommand(id, name, description) {
  const command = new CreateTask();
  command.setId(id);
  command.setName(name);
  command.setDescription(description);

  const commandType = new TypeUrl('type.spine.io/spine.web.test.CreateTask');

  return new TypedMessage(command, commandType);
}

function randomId(prefix) {
  const id = prefix + Math.round(Math.random() * 1000);
  const productId = new TaskId();
  productId.setValue(id);
  return productId;
}

function testEndpoint() {
  const httpClient = new HttpClient('https://spine-dev.appspot.com');
  return new HttpEndpoint(httpClient);
}

function newFirebaseClient() {
  return new FirebaseClient(firebase);
}

function newRequestFactory() {
  return new ActorRequestFactory('web-test-actor');
}

function newBackendClient() {
  return new BackendClient(testEndpoint(), newFirebaseClient(), newRequestFactory());
}

function fail(done) {
  return error => {
    console.error(error);
    assert.ok(false);
    done();
  };
}

const backendClient = newBackendClient();

describe('Client should', function () {

  // Big timeout due to remote calls during tests.
  this.timeout(2 * MINUTES);

  it('send commands successfully', done => {
    const productId = randomId('spine-web-test-1-');
    const command = creteTaskCommand(productId, 'Write tests', 'client-js needs tests; write\'em');

    backendClient.sendCommand(command, () => {

      const type = new TypeUrl('type.spine.io/spine.web.test.Task');
      const idType = new TypeUrl('type.spine.io/spine.web.test.TaskId');
      const typedId = new TypedMessage(productId, idType);

      backendClient.fetchById(type, typedId, data => {
        assert.equal(data.name, command.message.getName());
        assert.equal(data.description, command.message.getDescription());
        done();
      }, fail(done));

    }, fail(done), fail(done));
  });

  it('fetch all the existing entities of given type one by one', done => {
    const productId = randomId('spine-web-test-2-');
    const command = creteTaskCommand(productId, 'Run tests', 'client-js has tests; run\'em');

    backendClient.sendCommand(command, () => {

      let itemFound = false;
      const type = new TypeUrl('type.spine.io/spine.web.test.Task');

      backendClient.fetchAll({ofType: type}).oneByOne().subscribe({
        next(data) {
          // Ordering is not guaranteed by fetch and 
          // the list of entities cannot be cleaned for tests,
          // thus at least one of entities should match the target one.
          itemFound = data.id.value === productId.getValue() || itemFound;
        },
        error: fail(done),
        complete() {
          done();
        }
      });

    }, fail(done), fail(done));
  });

  it('fetch all the existing entities of given type at once', done => {
    const productId = randomId('spine-web-test-2-');
    const command = creteTaskCommand(productId, 'Run tests', 'client-js has tests; run\'em');

    backendClient.sendCommand(command, () => {

      const type = new TypeUrl('type.spine.io/spine.web.test.Task');
      backendClient.fetchAll({ofType: type}).atOnce()
        .then(data => {
          const targetObject = data.find(item => item.id.value === productId.getValue());
          assert.ok(targetObject);
          done();
        }, fail(done));

    }, fail(done), fail(done));
  });

  it('fails a malformed query', done => {
    const productId = randomId('spine-web-test-2-');
    const command = creteTaskCommand(productId, 'Run tests', 'client-js has tests; run\'em');

    backendClient.sendCommand(command, () => {

      const malformedType = new TypeUrl('/');
      backendClient.fetchAll({ofType: malformedType}).atOnce()
        .then(fail(done), error => {
          assert.ok(!error.isClient());
          assert.ok(error.isServer());
          done();
        });

    }, fail(done), fail(done));
  });

  it('fails a malformed command', done => {
    const malformedId = randomId(null);
    const command = creteTaskCommand(malformedId, 'Run tests', 'client-js has tests; run\'em');

    backendClient.sendCommand(command, fail(done), error => {
      assert.equal(error.code, 2);
      assert.equal(error.type, 'spine.core.CommandValidationError');
      assert.ok(error.validationError);
      done();
    }, fail(done));
  });
});
