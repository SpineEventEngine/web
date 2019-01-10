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

import uuid from 'uuid';
import assert from 'assert';

import {Message} from 'google-protobuf';
import {AnyPacker} from "../../src/client/any-packer";
import {Type, TypedMessage} from "../../src/client/typed-message";
import {Duration} from "../../src/client/time-utils";
import {Task, TaskId} from "../../proto/test/js/spine/web/test/given/task_pb";

class Given {
  constructor() {
    throw new Error('A utility Given class cannot be instantiated.');
  }

  static newTask() {
    const task = new Task();
    task.setId(Given.newTaskId());
    task.setName(uuid.v4());
    return task;
  }

  static newTaskId() {
    const taskId = new TaskId();
    taskId.setValue(uuid.v4());
    return taskId;
  }

  /**
   * @param {string} actual
   * @param {Type} expected
   */
  static assertTypeUrlForType(actual, expected) {
    assert.equal(actual, expected.url().value());
  }

  /**
   * @param {Message} actual
   * @param {Message} expected
   */
  static assertMessagesEqual(actual, expected) {
    assert.ok(Message.equals(actual, expected), 'Messages are expected to be identical.');
  }
}

Given.TYPE = {
  TASK_ID: Type.forClass(TaskId),
  TASK: Type.forClass(Task),
};

describe('AnyPacker', function () {

  const timeoutDuration = new Duration({seconds: 5});
  this.timeout(timeoutDuration.inMs());

  it('packs messages', () => {
    const task = Given.newTask();

    const any = AnyPacker.pack(task).as(Given.TYPE.TASK);
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Given.TYPE.TASK);

    const message = AnyPacker.unpack(any).as(Given.TYPE.TASK);
    Given.assertMessagesEqual(task, message);
  });

  it('packs a typed messages', () => {
    const task = Given.newTask();
    const typedTask = new TypedMessage(task, Given.TYPE.TASK);

    const any = AnyPacker.packTyped(typedTask);
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Given.TYPE.TASK);

    const message = AnyPacker.unpack(any).as(Given.TYPE.TASK);
    Given.assertMessagesEqual(task, message);
  });

  it ('packs an untyped message', () => {
    const task = Given.newTask();

    const any = AnyPacker.packMessage(task);
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Given.TYPE.TASK);

    const message = AnyPacker.unpack(any).as(Given.TYPE.TASK);
    Given.assertMessagesEqual(task, message);
  });

  it('packs a string', () => {
    const value = 'AnyPacker, I’m coming for you!';

    const any = AnyPacker.pack(value).asString();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.STRING);

    const actualValue = AnyPacker.unpack(any).asString();
    assert.equal(actualValue, value);
  });

  it('packs an int32', () => {
    const value = -33;

    const any = AnyPacker.pack(value).asInt32();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.INT32);

    const actualValue = AnyPacker.unpack(any).asInt32();
    assert.equal(actualValue, value);
  });

  it('packs an int64', () => {
    const value = -9223372036854775807;

    const any = AnyPacker.pack(value).asInt64();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.INT64);

    const actualValue = AnyPacker.unpack(any).asInt64();
    assert.equal(actualValue, value);
  });

  it('packs an uint32', () => {
    const value = 4294967295;

    const any = AnyPacker.pack(value).asUInt32();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.UINT32);

    const actualValue = AnyPacker.unpack(any).asUInt32();
    assert.equal(actualValue, value);
  });

  it('packs an uint64', () => {
    const value = Number.MAX_SAFE_INTEGER;

    const any = AnyPacker.pack(value).asUInt64();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.UINT64);

    const actualValue = AnyPacker.unpack(any).asUInt64();
    assert.equal(actualValue, value);
  });

  it('packs a float', () => {
    const value = 3.1235;
    const stringValue = value.toString();

    const any = AnyPacker.pack(value).asFloat();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.FLOAT);

    const actualValue = AnyPacker.unpack(any).asFloat();
    assert.ok(actualValue.toString().slice(0, stringValue.length), stringValue);
  });

  it('packs a double', () => {
    const value = 3.33333733335;

    const any = AnyPacker.pack(value).asDouble();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.DOUBLE);

    const actualValue = AnyPacker.unpack(any).asDouble();
    assert.equal(actualValue, value);
  });

  it('packs a boolean true value', () => {
    const value = true;

    const any = AnyPacker.pack(value).asBool();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.BOOL);

    const actualValue = AnyPacker.unpack(any).asBool();
    assert.ok(actualValue);
  });

  it('packs a boolean false value', () => {
    const value = false;

    const any = AnyPacker.pack(value).asBool();
    assert.ok(any);
    Given.assertTypeUrlForType(any.getTypeUrl(), Type.BOOL);

    const actualValue = AnyPacker.unpack(any).asBool();
    assert.ok(!actualValue);
  });
});
