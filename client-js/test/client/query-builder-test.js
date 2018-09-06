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

import {Message} from 'google-protobuf';
import {Type, TypedMessage} from '../../src/client/typed-message';
import {ActorRequestFactory, ColumnFilters} from '../../src/client/actor-request-factory';
import {AnyPacker} from '../../src/client/any-packer';
import {Duration} from '../../src/client/time-utils';
import {Task, TaskId} from '../../proto/test/js/spine/web/test/given/task_pb';
import {StringValue} from 'spine-web-client-proto/google/protobuf/wrappers_pb';
import {
  ColumnFilter,
  CompositeColumnFilter,
  EntityFilters
} from 'spine-web-client-proto/spine/client/entities_pb';

class Given {

  constructor() {
    throw new Error('A utility Given class cannot be instantiated.');
  }

  /**
   * @param {String} value
   * @return {TypedMessage}
   */
  static newTaskId(value) {
    const id = new TaskId();
    id.setValue(value);
    return new TypedMessage(id, Given.TYPE.TASK_ID);
  }

  /**
   * @param {String[]} values
   * @return {TypedMessage<TaskId>[]}
   */
  static newTaskIds(values) {
    return values.map(Given.newTaskId);
  }

  /**
   * @param {ActorContext} context
   */
  static assertActorContextCorrect(context) {
    assert.ok(context);
    assert.ok(context.getTimestamp().getSeconds() <= new Date().getTime());
    assert.equal(context.getActor().getValue(), Given.ACTOR);
  }

  /**
   * @param {Array} actual
   * @param {Array} expected
   */
  static assertUnorderedEqual(actual, expected) {
    assert.ok(actual.length, expected.length, 'Arrays are expected to be of the same size.');
    expected.forEach(expectedItem => {
      assert.ok(actual.includes(expectedItem), 'An item is expected to be included in array.');
    });
  }

  /**
   * @param {Target} target
   * @param {Type} type
   */
  static assertTargetTypeEqual(target, type) {
    assert.equal(target.getType(), type.url().value());
  }

  /**
   * @param {Message} actual
   * @param {Message} expected
   */
  static assertMessagesEqual(actual, expected) {
    assert.ok(Message.equals(actual, expected), 'Messages are expected to be identical.');
  }

  /**
   * @return {ActorRequestFactory}
   */
  static requestFactory() {
    return new ActorRequestFactory(Given.ACTOR);
  }
}

Given.TYPE = {
  TASK_ID: Type.of(TaskId, 'type.spine.io/spine.web.test.given.TaskId'),
  TASK: Type.of(Task, 'type.spine.io/spine.web.test.given.Task'),
};
Given.ACTOR = 'spine-web-client-test-actor';

describe('QueryBuilder', function () {

  const timeoutDuration = new Duration({seconds: 5});
  this.timeout(timeoutDuration.inMs());

  it('creates a Query of query for type', done => {
    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    done();
  });

  /********* IDs *********/

  it('creates a Query for type with with no IDs', done => {
    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .byIds([])
      .build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    done();
  });

  it('creates a Query for type with multiple IDs', done => {
    const values = ['meeny', 'miny', 'moe'];
    const taskIds = Given.newTaskIds(values);

    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .byIds(taskIds).build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const filters = target.getFilters();
    assert.ok(filters);
    assert.ok(filters.getFilterList().length === 0);

    const idFilter = filters.getIdFilter();
    assert.ok(idFilter);

    const targetIds = idFilter.getIdsList()
      .map(entityId => entityId.getId())
      .map(any => AnyPacker.unpack(any).as(Given.TYPE.TASK_ID))
      .map(taskId => taskId.getValue());

    Given.assertUnorderedEqual(targetIds, values);

    done();
  });

  it('creates a Query for type with string IDs', done => {
    const values = ['meeny', 'miny', 'moe'];

    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .byIds(values).build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const filters = target.getFilters();
    assert.ok(filters);
    assert.ok(filters.getFilterList().length === 0);

    const idFilter = filters.getIdFilter();
    assert.ok(idFilter);

    const targetIds = idFilter.getIdsList()
      .map(entityId => entityId.getId())
      .map(any => AnyPacker.unpack(any).asString());

    Given.assertUnorderedEqual(targetIds, values);

    done();
  });

  it('creates a Query for type with number IDs', done => {
    const values = [29, 99971, 104729];

    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .byIds(values).build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const filters = target.getFilters();
    assert.ok(filters);
    assert.ok(filters.getFilterList().length === 0);

    const idFilter = filters.getIdFilter();
    assert.ok(idFilter);

    const targetIds = idFilter.getIdsList()
      .map(entityId => entityId.getId())
      .map(any => AnyPacker.unpack(any).asInt64());

    Given.assertUnorderedEqual(targetIds, values);

    done();
  });

  it('throws an error on multiple #byIds() invocations', done => {
    const firstIds = Given.newTaskIds(['tick']);
    const secondIds = Given.newTaskIds(['tock']);
    try {
      Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .byIds(firstIds)
        .byIds(secondIds);
      done(new Error('#byIds() multiple invocations did not result in error.'));
    } catch (error) {
      done();
    }
  });

  it('throws an error if #byIds() is invoked with non-Array value', done => {
    try {
      Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .byIds({error: true});
      done(new Error('#byIds() non-Array value did not result in error.'));
    } catch (error) {
      done();
    }
  });

  it('throws an error if #byIds() is invoked with non-TypedMessage IDs', done => {
    try {
      Given.requestFactory().query()
        .select(Given.TYPE.TASK)
        .byIds([{tinker: 'tailor'}, {soldier: 'sailor'}]);
      done(new Error('#byIds() non-TypedMessage IDs did not result in error.'));
    } catch (error) {
      done();
    }
  });

  /********* FILTERS *********/

  it('creates a Query with a no filters', done => {
    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .where([])
      .build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    done();
  });

  it('creates a Query with a single ColumnFilter', done => {
    const nameFilter = ColumnFilters.eq('name', TypedMessage.string('Implement tests'));
    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .where([nameFilter])
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(!target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const expectedFilters = new EntityFilters();
    expectedFilters.setFilterList([ColumnFilters.all([nameFilter])]);

    Given.assertMessagesEqual(target.getFilters(), expectedFilters);

    done();
  });

  it('creates a Query with a multiple ColumnFilter', done => {
    const nameFilter = ColumnFilters.eq('name', TypedMessage.string('Implement tests'));
    const descriptionFilter = ColumnFilters.eq(
      'description', TypedMessage.string('Web needs tests, eh?')
    );
    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .where([nameFilter, descriptionFilter])
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(!target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const expectedFilters = new EntityFilters();
    expectedFilters.setFilterList([ColumnFilters.all([nameFilter, descriptionFilter])]);

    Given.assertMessagesEqual(target.getFilters(), expectedFilters);

    done();
  });

  it('creates a Query with a single CompositeColumnFilter', done => {
    const nameFilter1 = ColumnFilters.eq('name', TypedMessage.string('Implement tests'));
    const nameFilter2 = ColumnFilters.eq('name', TypedMessage.string('Create a PR'));
    const compositeColumnFilter = ColumnFilters.either([nameFilter1, nameFilter2]);
    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .where([compositeColumnFilter])
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(!target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const expectedFilters = new EntityFilters();
    expectedFilters.setFilterList([compositeColumnFilter]);

    Given.assertMessagesEqual(target.getFilters(), expectedFilters);

    done();
  });

  it('creates a Query with a multiple CompositeColumnFilters', done => {
    const nameFilter1 = ColumnFilters.eq('name', TypedMessage.string('Implement tests'));
    const nameFilter2 = ColumnFilters.eq('name', TypedMessage.string('Create a PR'));
    const nameFilter = ColumnFilters.either([nameFilter1, nameFilter2]);
    const descriptionFilter = ColumnFilters.all([
      ColumnFilters.eq('description', TypedMessage.string('Web needs tests, eh?')),
    ]);

    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .where([nameFilter, descriptionFilter])
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(!target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const expectedFilters = new EntityFilters();
    expectedFilters.setFilterList([nameFilter, descriptionFilter]);

    Given.assertMessagesEqual(target.getFilters(), expectedFilters);

    done();
  });

  it('throws an error if #where() is invoked with non-Array value', done => {
    const nameFilter = ColumnFilters.eq('name', TypedMessage.string('Implement tests'));

    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .where(nameFilter);
      done(new Error('An error was expected due to invalid #where() parameter.'));
    } catch (e) {
      done();
    }
  });

  it('throws an error if #where() is invoked with non-filter values', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .where(['Duck', 'duck', 'goose']);
      done(new Error('An error was expected due to invalid #where() parameter.'));
    } catch (e) {
      done();
    }
  });

  it('throws an error if #where() is invoked with mixed ColumnFilter and CompositeColumnFilter values', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .where([new ColumnFilter(), new CompositeColumnFilter()]);
      done(new Error('An error was expected due to mixed column filter types.'));
    } catch (e) {
      done();
    }
  });

  it('throws an error if #where() is invoked more than once', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .where([new ColumnFilter()])
        .where([new ColumnFilter()]);
      done(new Error('An error was expected due to multiple #where() invocations.'));
    } catch (e) {
      done();
    }
  });

  /********* MASKS *********/

  it('creates a Query with a provided field mask', done => {
    const maskedFields = ['id', 'description'];
    const query = Given.requestFactory()
      .query()
      .select(Given.TYPE.TASK)
      .withMask(maskedFields)
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    Given.assertUnorderedEqual(query.getFieldMask().getPathsList(), maskedFields);

    done();
  });

  it('throws an error if #withMask() is invoked more than once', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .withMask(['name'])
        .withMask(['description']);
      done(new Error('An error was expected due to multiple #withMask() invocations.'))
    } catch (e) {
      done();
    }
  });

  it('throws an error if #withMask() is invoked with non-Array value', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .withMask('name');
      done(new Error('An error was expected due to invalid #withMask() argument.'))
    } catch (e) {
      done();
    }
  });

  it('throws an error if #withMask() is invoked with non-string field names', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.TYPE.TASK)
        .withMask([22]);
      done(new Error('An error was expected due to invalid #withMask() argument.'))
    } catch (e) {
      done();
    }
  });
});
