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
import uuid from 'uuid';

import {devFirebaseApp} from './test-firebase-app';
import {Type} from '@lib/client/typed-message';
import {Duration} from '@lib/client/time-utils';

import {CreateTask, RenameTask} from '@testProto/spine/web/test/given/commands_pb';
import {Task, TaskId} from '@testProto/spine/web/test/given/task_pb';
import * as testProtobuf from '@testProto/index';
import {Filter, CompositeFilter} from '@proto/spine/client/filters_pb';
import {Topic} from '@testProto/spine/client/subscription_pb';
import {Project} from '@testProto/spine/web/test/given/project_pb';
import {FirebaseBackendClient} from '@lib/client/firebase-backend-client';
import {ActorProvider} from '@lib/client/actor-request-factory';
import {UserId} from '@proto/spine/core/user_id_pb';
import {
 ServerError,
 CommandValidationError,
 CommandHandlingError,
 ConnectionError
} from '@lib/client/errors';
import {fail} from './test-helpers';

class Given {

  constructor() {
    throw new Error('A utility Given class cannot be instantiated.');
  }

  static client(endpoint = 'https://spine-dev.appspot.com') {
    return FirebaseBackendClient
      .forProtobufTypes(testProtobuf)
      .usingFirebase({
        atEndpoint: endpoint,
        withFirebaseStorage: devFirebaseApp,
        forActor: new ActorProvider()
      });
  }

  /**
   * @param {?String} withId
   * @param {?String} withPrefix
   * @param {?String} named
   * @param {?String} describedAs
   *
   * @return {CreateTask}
   */
  static createTaskCommand({withId: id, withPrefix: idPrefix, named: name, describedAs: description}) {
    const taskId = this.taskId({value: id, withPrefix: idPrefix});

    name = typeof name === 'undefined' ? this.DEFAULT_TASK_NAME : name;
    description = typeof description === 'undefined' ? this.DEFAULT_TASK_DESCRIPTION : description;

    const command = new CreateTask();
    command.setId(taskId);
    command.setName(name);
    command.setDescription(description);

    return command;
  }

  /**
   * @param {!String[]} named
   * @param {?String} withPrefix
   *
   * @return {CreateTask[]}
   */
  static createTaskCommands({named: names, withPrefix: idPrefix}) {
    const commands = [];
    for (let i = 0; i < names.length; i++) {
      const command = this.createTaskCommand({
        withPrefix: idPrefix,
        named: names[i]
      });
      commands.push(command);
    }
    return commands;
  }

  /**
   * @param {!String} withId
   * @param {!String} to
   *
   * @return {RenameTask}
   */
  static renameTaskCommand({withId: id, to: newName}) {
    const taskId = this.taskId({value: id});

    const command = new RenameTask();
    command.setId(taskId);
    command.setName(newName);

    return command;

  }

  /**
   * @param value
   * @param withPrefix
   *
   * @return {TaskId}
   */
  static taskId({value, withPrefix: prefix}) {
    if (typeof value === 'undefined') {
      value = uuid.v4();
    }
    if (typeof prefix !== 'undefined') {
      value = `${prefix}-${value}`;
    }
    const taskId = new TaskId();
    taskId.setValue(value);
    return taskId;
  }

  /**
   * A function that does nothing.
   */
  static noop() {
    // Do nothing.
  }
}

Given.DEFAULT_TASK_NAME = 'Get to Mount Doom';
Given.DEFAULT_TASK_DESCRIPTION = 'There seems to be a bug with the rings that needs to be fixed';
Given.TYPE = {
  OF_ENTITY: {
    TASK: Type.forClass(Task),
    PROJECT: Type.forClass(Project),
  },
  MALFORMED: Type.of(Object, 'types.spine.io/malformed'),
};

describe('FirebaseBackendClient', function () {

  const client = Given.client();

  // Big timeout due to remote calls during tests.
  const timeoutDuration = new Duration({minutes: 2});
  this.timeout(timeoutDuration.inMs());

  it('sends commands successfully', done => {

    const command = Given.createTaskCommand({
      withIdPrefix: 'spine-web-test-send-command',
      named: 'Implement Spine Web JS client tests',
      describedAs: 'Spine Web need integration tests'
    });

    const taskId = command.getId();

    client.sendCommand(command, () => {

      client.fetchById(Given.TYPE.OF_ENTITY.TASK, taskId, data => {
        assert.equal(data.getId().getValue(), taskId);
        assert.equal(data.getName(), command.getName());
        assert.equal(data.getDescription(), command.getDescription());

        done();

      }, fail(done));

    }, fail(done), fail(done));
  });

  it('fails command sending when wrong server endpoint specified', done => {
    const fakeBaseUrl = 'https://malformed-server-endpoint.com';
    const malformedBackendClient = Given.client(fakeBaseUrl);
    const command = Given.createTaskCommand({
      withIdPrefix: 'spine-web-test-send-command',
      named: 'Implement Spine Web JS client tests',
      describedAs: 'Spine Web need integration tests'
    });

    malformedBackendClient.sendCommand(
      command,
      fail(done, 'A command was acknowledged when it was expected to fail.'),
      error => {
        assert.ok(error instanceof CommandHandlingError);
        assert.ok(error.message.startsWith(`request to ${fakeBaseUrl}/command failed`));
        const connectionError = error.getCause();
        assert.ok(connectionError instanceof ConnectionError);
        done();
      },
      fail(done, 'A command was rejected when an error was expected.'));
  });

  it('fails with `CommandValidationError` for invalid command', done => {
    const command = Given.createTaskCommand({withId: null});

    client.sendCommand(
      command,
      fail(done, 'A command was acknowledged when it was expected to fail.'),
      error => {
        assert.ok(error instanceof CommandValidationError);
        assert.ok(error.validationError());
        assert.ok(error.assuresCommandNeglected());

        const cause = error.getCause();
        assert.ok(cause);
        assert.equal(cause.getCode(), 2);
        assert.equal(cause.getType(), 'spine.core.CommandValidationError');
        done();
      },
      fail(done, 'A command was rejected when an error was expected.'));
  });

  it('returns `null` as a value when fetches entity by ID that is missing', done => {

    const taskId = Given.taskId({});

    client.fetchById(Given.TYPE.OF_ENTITY.TASK, taskId, data => {
      assert.equal(data, null);

      done();

    }, fail(done));

  });

  it('fetches all the existing entities of Given type one by one', done => {
    const command = Given.createTaskCommand({withPrefix: 'spine-web-test-one-by-one'});
    const taskId = command.getId();

    client.sendCommand(command, () => {

      let itemFound = false;

      client.fetchAll({ofType: Given.TYPE.OF_ENTITY.TASK}).oneByOne().subscribe({
        next(data) {
          // Ordering is not guaranteed by fetch and
          // the list of entities cannot be cleaned for tests,
          // thus at least one of entities should match the target one.
          itemFound = data.getId().getValue() === taskId.getValue() || itemFound;
        },
        error: fail(done),
        complete() {
          assert.ok(itemFound);
          done();
        }
      });

    }, fail(done), fail(done));
  });

  it('fetches all the existing entities of Given type at once', done => {
    const command = Given.createTaskCommand({withPrefix: 'spine-web-test-at-once'});
    const taskId = command.getId();

    client.sendCommand(command, () => {

      client.fetchAll({ofType: Given.TYPE.OF_ENTITY.TASK}).atOnce()
        .then(data => {
          const targetObject = data.find(item => item.getId().getValue() === taskId.getValue());
          assert.ok(targetObject);
          done();
        }, fail(done));

    }, fail(done), fail(done));
  });

  it('fetches an empty list for entity that does not get created at once', done => {
    client.fetchAll({ofType: Given.TYPE.OF_ENTITY.PROJECT}).atOnce()
      .then(data => {
        assert.ok(data.length === 0);
        done();
      }, fail(done));
  });

  it('fetches an empty list for entity that does not get created one-by-one', done => {
    client.fetchAll({ofType: Given.TYPE.OF_ENTITY.PROJECT}).oneByOne()
      .subscribe({
        next: fail(done),
        error: fail(done),
        complete: () => done()
      });
  });

  it('fails a malformed query', done => {
    const command = Given.createTaskCommand({withPrefix: 'spine-web-test-malformed-query'});

    client.sendCommand(command, () => {

      client.fetchAll({ofType: Given.TYPE.MALFORMED}).atOnce()
        .then(fail(done), error => {
          assert.ok(error instanceof ServerError);
          assert.equal(error.message, 'Internal Server Error');
          done();
        });

    }, fail(done), fail(done));
  });

  it('subscribes to new entities of type', done => {
    const names = ['Task #1', 'Task #2', 'Task #3'];
    const tasksToBeCreated = names.length;
    let taskIds;
    let count = 0;
    client.subscribeToEntities({ofType: Given.TYPE.OF_ENTITY.TASK})
      .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
        itemAdded.subscribe({
          next: task => {
            const id = task.getId().getValue();
            console.log(`Retrieved task '${id}'`);
            if (taskIds.includes(id)) {
              count++;
              if (count === tasksToBeCreated) {
                unsubscribe();
                done();
              }
            }
          }
        });
        itemRemoved.subscribe({
          next: fail(done, 'Unexpected entity remove during entity create subscription test.')
        });
        itemChanged.subscribe({
          next: fail(done, 'Unexpected entity change during entity create subscription test.')
        });
      })
      .catch(fail(done));

    const commands = Given.createTaskCommands({
      withPrefix: 'spine-web-test-subscribe',
      named: names
    });
    taskIds = commands.map(command => command.getId().getValue());
    commands.forEach(command => {
      client.sendCommand(command, Given.noop, fail(done), fail(done));
    });
  });

  it('subscribes to entity changes of type', done => {
    const TASKS_TO_BE_CHANGED = 3;
    let taskIds;
    let countChanged = 0;
    const initialTaskNames = ['Created task #1', 'Created task #2', 'Created task #3'];

    client.subscribeToEntities({ofType: Given.TYPE.OF_ENTITY.TASK})
      .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
        itemAdded.subscribe({
          next: item => {
            const id = item.getId().getValue();
            console.log(`Retrieved new task '${id}'.`);
            if (taskIds.includes(id)) {
              assert.ok(
                initialTaskNames.includes(item.getName()),
                `Task is named "${item.getName()}", expected one of [${initialTaskNames}]`
              );
            }
          }
        });
        itemRemoved.subscribe({
          next: fail(done, 'Task was removed in a test of entity changes subscription.')
        });
        itemChanged.subscribe({
          next: item => {
            const id = item.getId().getValue();
            if (taskIds.includes(id)) {
              console.log(`Got task changes for ${id}.`);
              countChanged++;
              if (countChanged === TASKS_TO_BE_CHANGED) {
                unsubscribe();
                done();
              }
            } else {
              done(new Error('Unexpected entity changes during subscription to entity changes test'));
            }
          }
        });
      })
      .catch(fail(done));

    // Create tasks.
    const createCommands = Given.createTaskCommands({
      count: TASKS_TO_BE_CHANGED,
      withPrefix: 'spine-web-test-subscribe',
      named: initialTaskNames
    });
    taskIds = createCommands.map(command => command.getId().getValue());
    const createPromises = [];
    createCommands.forEach(command => {
      const promise = new Promise(resolve => {
        client.sendCommand(
          command,
          () => {
            console.log(`Task '${command.getId().getValue()}' created.`);
            resolve();
          },
          fail(done, 'Unexpected error while creating a task.'),
          fail(done, 'Unexpected rejection while creating a task.')
        );
      });
      createPromises.push(promise);
    });

    // Rename created tasks.
    Promise.all(createPromises).then(() => {
      // Rename tasks in a timeout after they are created to
      // allow for added subscriptions to be updated first.
      const renameTimeout = new Duration({seconds: 30});
      setTimeout(() => {
        taskIds.forEach(taskId => {
          const renameCommand = Given.renameTaskCommand({
            withId: taskId,
            to: `Renamed '${taskId}'`
          });
          client.sendCommand(
            renameCommand,
            () => console.log(`Task '${taskId}' renamed.`),
            fail(done, 'Unexpected error while renaming a task.'),
            fail(done, 'Unexpected rejection while renaming a task.')
          );
        });
      }, renameTimeout.inMs());
    });
  });

  it('subscribes to entity changes by id', done => {
    const expectedChangesCount = 2;
    const initialTaskName = 'Initial task name';
    const expectedRenames = ['Renamed once', 'Renamed twice'];

    // Create tasks.
    const createCommand = Given.createTaskCommand({
      withPrefix: 'spine-web-test-subscribe',
      named: initialTaskName
    });
    const taskId = createCommand.getId();
    const taskIdValue = createCommand.getId().getValue();

    const promise = new Promise(resolve => {
      client.sendCommand(
        createCommand,
        () => {
          console.log(`Task '${taskIdValue}' created.`);
          resolve();
        },
        fail(done, 'Unexpected error while creating a task.'),
        fail(done, 'Unexpected rejection while creating a task.')
      );
    });

    let changesCount = 0;
    client.subscribeToEntities({ofType: Given.TYPE.OF_ENTITY.TASK, byId: taskId})
      .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
        itemAdded.subscribe({
          next: item => {
            const id = item.getId().getValue();
            console.log(`Retrieved new task '${id}'.`);
            if (taskIdValue === id) {
              assert.equal(
                item.getName(), initialTaskName,
                `Task is named "${item.getName()}", expected "${initialTaskName}"`
              );
            } else {
              done(new Error(`Only changes for task with ID ${taskIdValue} should be received.`))
            }
          }
        });
        itemRemoved.subscribe({
          next: fail(done, 'Task was removed in a test of entity changes subscription.')
        });
        itemChanged.subscribe({
          next: item => {
            const id = item.getId().getValue();
            if (taskIdValue === id) {
              console.log(`Got task changes for ${id}.`);
              assert.equal(item.getName(), expectedRenames[changesCount]);
              changesCount++;
              if (changesCount === expectedChangesCount) {
                unsubscribe();
                done();
              }
            } else {
              done(new Error('Unexpected entity changes during subscription to entity changes test'));
            }
          }
        });
      })
      .catch(fail(done));

    // Rename created task.
    const renameTimeout = new Duration({seconds: 20});
    promise.then(() => {
      // Tasks are renamed with a timeout after to allow for changes to show up in subscriptions.
      return new Promise(resolve => {
        setTimeout(() => {
          const renameCommand = Given.renameTaskCommand({
            withId: taskIdValue,
            to: 'Renamed once'
          });
          client.sendCommand(
            renameCommand,
            () => {
              resolve();
              console.log(`Task '${taskIdValue}' renamed for the first time.`)
            },
            fail(done, 'Unexpected error while renaming a task.'),
            fail(done, 'Unexpected rejection while renaming a task.')
          );
        }, renameTimeout.inMs());
      });
    }).then(() => {
      setTimeout(() => {
        const renameCommand = Given.renameTaskCommand({
          withId: taskIdValue,
          to: 'Renamed twice'
        });
        client.sendCommand(
          renameCommand,
          () => console.log(`Task '${taskIdValue}' renamed for the second time.`),
          fail(done, 'Unexpected error while renaming a task.'),
          fail(done, 'Unexpected rejection while renaming a task.')
        );
      }, renameTimeout.inMs());
    });
  });

  it('fails a malformed subscription', done => {
    client.subscribeToEntities({ofType: Given.TYPE.MALFORMED})
      .then(() => {
        done(new Error('A malformed subscription should not yield results.'));
      })
      .catch(error => {
        assert.ok(true);
        done();
      });
  });
});
