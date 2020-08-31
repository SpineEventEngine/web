/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import TestEnvironment from '../given/test-environment';
import {CommandHandlingError, CommandValidationError, ConnectionError} from '@lib/index';
import {CreateTask} from '@testProto/spine/web/test/given/commands_pb';
import {TaskCreated} from '@testProto/spine/web/test/given/events_pb';
import {Task} from '@testProto/spine/web/test/given/task_pb';
import {fail} from '../test-helpers';
import {client, initClient} from './given/firebase-client';
import {AnyPacker} from '@lib/client/any-packer';
import {TenantProvider} from '@lib/client/tenant';
import {Type} from '@lib/client/typed-message';

describe('FirebaseClient command sending', function () {

  // Big timeout allows to receive model state changes during tests.
  this.timeout(5000);

  it('completes with success', done => {

    const command = TestEnvironment.createTaskCommand({
      withPrefix: 'spine-web-test-send-command',
      named: 'Implement Spine Web JS client tests',
      describedAs: 'Spine Web need integration tests'
    });

    const taskId = command.getId();

    const fetchAndCheck = () => {
      client.select(Task)
          .byId(taskId)
          .run()
          .then(data => {
            assert.equal(data.length, 1);
            const item = data[0];
            assert.equal(item.getId().getValue(), taskId);
            assert.equal(item.getName(), command.getName());
            assert.equal(item.getDescription(), command.getDescription());

            done();

          }, fail(done));
    };

    client.command(command)
        .onOk(fetchAndCheck)
        .onError(fail(done))
        .onImmediateRejection(fail(done))
        .post();
  });

  it('fails when wrong server endpoint specified', done => {
    const fakeBaseUrl = 'https://malformed-server-endpoint.com';
    const malformedBackendClient =
        initClient(fakeBaseUrl, new TenantProvider(TestEnvironment.tenantId()));
    const command = TestEnvironment.createTaskCommand({
      withPrefix: 'spine-web-test-send-command',
      named: 'Implement Spine Web JS client tests',
      describedAs: 'Spine Web need integration tests'
    });

    const checkError = error => {
      try {
        assert.ok(error instanceof CommandHandlingError);
        assert.ok(error.message.startsWith(`request to ${fakeBaseUrl}/command failed`));
        const connectionError = error.getCause();
        assert.ok(connectionError instanceof ConnectionError);
        done();
      } catch (e) {
        fail(done, e.message)
      }
    };
    malformedBackendClient.command(command)
        .onOk(fail(done, 'A command was acknowledged when it was expected to fail.'))
        .onError(checkError)
        .onImmediateRejection(fail(done, 'A command was rejected when an error was expected.'))
        .post();
  });

  it('fails with `CommandValidationError` for invalid command message', done => {
    const command = TestEnvironment.createTaskCommand({withId: null});

    const checkError = error => {
      try {
        assert.ok(error instanceof CommandValidationError);
        assert.ok(error.validationError());
        assert.ok(error.assuresCommandNeglected());

        const cause = error.getCause();
        assert.ok(cause);
        assert.equal(cause.getCode(), 2);
        assert.equal(cause.getType(), 'spine.core.CommandValidationError');
        done();
      } catch (e) {
        fail(done, e.message)
      }
    };

    client.command(command)
        .onOk(fail(done, 'A command was acknowledged when it was expected to fail.'))
        .onError(checkError)
        .onImmediateRejection(fail(done, 'A command was rejected when an error was expected.'))
        .post();
  });

  it('allows to observe the produced events of a given type', done => {
    const taskName = 'Implement Spine Web JS client tests';
    const command = TestEnvironment.createTaskCommand({
      withPrefix: 'spine-web-test-send-command',
      named: taskName,
      describedAs: 'Spine Web need integration tests'
    });

    const taskId = command.getId();

    client.command(command)
        .onError(fail(done))
        .onImmediateRejection(fail(done))
        .observe(TaskCreated, ({subscribe, unsubscribe}) => {
          subscribe(event => {
            const packedMessage = event.getMessage();
            const taskCreatedType = Type.forClass(TaskCreated);
            const message = AnyPacker.unpack(packedMessage).as(taskCreatedType);
            const theTaskId = message.getId().getValue();
            assert.equal(
                taskId, theTaskId,
                `Expected the task ID to be '${taskId}', got '${theTaskId}' instead.`
            );
            const theTaskName = message.getName();
            assert.equal(
                taskName, theTaskName,
                `Expected the task name to be '${taskName}', got '${theTaskName}' instead.`
            );
            const origin = event.getContext().getPastMessage().getMessage();
            const originType = origin.getTypeUrl();
            const createTaskType = Type.forClass(CreateTask);
            const expectedOriginType = createTaskType.url().value();
            assert.equal(
                expectedOriginType, originType,
                `Expected origin to be of type '${expectedOriginType}', got 
                            '${originType}' instead.`
            );
            unsubscribe();
            done();
          });
        })
        .post()
  });

  it('fails when trying to observe a malformed event type', done => {
    const Unknown = class {
      static typeUrl() {
        return 'spine.web/fails.malformed.type'
      }
    };

    const command = TestEnvironment.createTaskCommand({
      withPrefix: 'spine-web-test-send-command',
      named: 'Implement Spine Web JS client tests',
      describedAs: 'Spine Web need integration tests'
    });

    client.command(command)
        .onError(fail(done))
        .onImmediateRejection(fail(done))
        .observe(Unknown)
        .post()
        .then(() => {
          done(new Error('An attempt to observe a malformed event type did not lead to an ' +
              'error.'));
        })
        .catch(() => {
              assert.ok(true);
              done();
            }
        );
  });
});
