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

import {devFirebaseApp} from './test-firebase-app';
import {TypedMessage, TypeUrl} from '../../src/client/typed-message';

import {CreateTask} from '../../proto/test/js/spine/web/test/given/commands_pb';
import {TaskId} from '../../proto/test/js/spine/web/test/given/task_pb';
import {Topic} from '../../proto/test/js/spine/client/subscription_pb';
import {BackendClient} from '../../src/client/backend-client';

const MILLISECONDS = 1;
const SECONDS = 1000 * MILLISECONDS;
const MINUTES = 60 * SECONDS;

const PROJECT_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.web.test.given.Project');
const CREATE_TASK_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.web.test.given.CreateTask');
const TASK_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.web.test.given.Task');
const TASK_ID_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.web.test.given.TaskId');

function createTaskCommand(id, name, description) {
  const command = new CreateTask();
  command.setId(id);
  command.setName(name);
  command.setDescription(description);

  return new TypedMessage(command, CREATE_TASK_MESSAGE_TYPE);
}

function randomId(prefix) {
  const id = prefix + Math.round(Math.random() * 1000);
  const productId = new TaskId();
  productId.setValue(id);
  return productId;
}

function newBackendClient() {
  return BackendClient.usingFirebase({
    atEndpoint: 'https://spine-dev.appspot.com',
    withFirebaseStorage: devFirebaseApp,
    forActor: 'web-test-actor'
  });
}

function fail(done) {
  return error => {
    done(new Error(`Test failed. Cause: ${error ? JSON.stringify(error) : 'not identified'}`));
  };
}

const backendClient = newBackendClient();

describe('Client should', function () {

  // Big timeout due to remote calls during tests.
  this.timeout(2 * MINUTES);

  it('send commands successfully', done => {
    const productId = randomId('spine-web-test-send-command-');
    const command = createTaskCommand(productId, 'Write tests', 'client-js needs tests; write\'em');

    backendClient.sendCommand(command, () => {

      const typedId = new TypedMessage(productId, TASK_ID_MESSAGE_TYPE);

      backendClient.fetchById(TASK_MESSAGE_TYPE, typedId, data => {
        assert.equal(data.name, command.message.getName());
        assert.equal(data.description, command.message.getDescription());
        done();
      }, fail(done));

    }, fail(done), fail(done));
  });

  it('fails a malformed command', done => {
    const malformedId = randomId(null);
    const command = createTaskCommand(malformedId, 'Run tests', 'client-js has tests; run\'em');

    backendClient.sendCommand(command, fail(done), error => {
      assert.equal(error.code, 2);
      assert.equal(error.type, 'spine.core.CommandValidationError');
      assert.ok(error.validationError);
      done();
    }, fail(done));
  });

  it('fetch all the existing entities of given type one by one', done => {
    const productId = randomId('spine-web-test-one-by-one-');
    const command = createTaskCommand(productId, 'Run tests', 'client-js has tests; run\'em');

    backendClient.sendCommand(command, () => {

      let itemFound = false;

      backendClient.fetchAll({ofType: TASK_MESSAGE_TYPE}).oneByOne().subscribe({
        next(data) {
          // Ordering is not guaranteed by fetch and 
          // the list of entities cannot be cleaned for tests,
          // thus at least one of entities should match the target one.
          itemFound = data.id.value === productId.getValue() || itemFound;
        },
        error: fail(done),
        complete() {
          assert.ok(itemFound);
          done();
        }
      });

    }, fail(done), fail(done));
  });

  it('fetch all the existing entities of given type at once', done => {
    const productId = randomId('spine-web-test-at-once-');
    const command = createTaskCommand(productId, 'Run tests', 'client-js has tests; run\'em');

    backendClient.sendCommand(command, () => {

      const type = new TypeUrl('type.spine.io/spine.web.test.given.Task');
      backendClient.fetchAll({ofType: type}).atOnce()
        .then(data => {
          const targetObject = data.find(item => item.id.value === productId.getValue());
          assert.ok(targetObject);
          done();
        }, fail(done));

    }, fail(done), fail(done));
  });

  it('fetch an empty list for entity that does not get created at once', done => {
    backendClient.fetchAll({ofType: PROJECT_MESSAGE_TYPE}).atOnce()
      .then(data => {
        assert.ok(data.length === 0);
        done();
      }, fail(done));
  });

  it('fetch an empty list for entity that does not get created one-by-one', done => {
    backendClient.fetchAll({ofType: PROJECT_MESSAGE_TYPE}).oneByOne()
      .subscribe({
        next: fail(done),
        error: fail(done),
        complete: () => done()
      });
  });

  it('fails a malformed query', done => {
    const productId = randomId('spine-web-test-malformed-query-');
    const command = createTaskCommand(productId, 'Run tests', 'client-js has tests; run\'em');

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

  it('subscribes to entity changes', done => {
    const malformedId = randomId(null);
    let count = 0;
    backendClient.subscribeToEntities({ofType: TASK_MESSAGE_TYPE})
      .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
        itemAdded.subscribe({
          next: item => {
            count++;
            if (count === 3) {
              done();
            }
          }
        });
        itemRemoved.subscribe({next: fail(done)});
        itemChanged.subscribe({next: fail(done)});
      })
      .catch(fail(done));
    const noop = () => {
    };
    const productId1 = randomId('spine-web-test-subscribe-');
    const command1 = createTaskCommand(productId1, 'Run tests', 'client-js has tests; run\'em');
    backendClient.sendCommand(command1, noop, noop, noop);
    const productId2 = randomId('spine-web-test-subscribe-');
    const command2 = createTaskCommand(productId2, 'Run tests', 'client-js has tests; run\'em');
    backendClient.sendCommand(command2, noop, noop, noop);
    const productId3 = randomId('spine-web-test-subscribe-');
    const command3 = createTaskCommand(productId3, 'Run tests', 'client-js has tests; run\'em');
    backendClient.sendCommand(command3, noop, noop, noop);
  });

  it('fails a malformed subscription', done => {
    const malformedType = new TypeUrl('type.spine.io/malformed');
    backendClient.subscribeToEntities({ofType: malformedType})
      .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
        done(new Error('Unexpected result received'));
      })
      .catch(error => {
        assert.ok(true);
        done();
      });
  });
});
