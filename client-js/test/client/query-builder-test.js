/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import {Message} from 'google-protobuf';
import {Type} from '@lib/client/typed-message';
import {ActorProvider, ActorRequestFactory, Filters} from '@lib/client/actor-request-factory';
import {AnyPacker} from '@lib/client/any-packer';
import {Duration} from '@lib/client/time-utils';
import {OrderBy} from '@proto/spine/client/query_pb';
import {ActorContext} from '@proto/spine/core/actor_context_pb';
import {Task, TaskId} from '@testProto/spine/test/js/task_pb';
import {CompositeFilter, Filter, Target, TargetFilters} from '@proto/spine/client/filters_pb';

class Given {

  constructor() {
    throw new Error('A utility Given class cannot be instantiated.');
  }

  /**
   * @param {String} value
   * @return {TaskId}
   */
  static newTaskId(value) {
    const id = new TaskId();
    id.setValue(value);
    return id;
  }

  /**
   * @param {String[]} values
   * @return {TaskId[]}
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
    assert.strictEqual(context.getActor(), ActorProvider.ANONYMOUS);
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
    assert.strictEqual(target.getType(), type.url().value());
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
    return new ActorRequestFactory(new ActorProvider());
  }
}

class Ordering {

  /**
   * Returns the first element of the list of `OrderBy` elements.
   *
   * @param {Array<OrderBy>} orderByList
   * @returns {OrderBy}
   */
  static firstOrderBy(orderByList) {
    return orderByList[0]
  }
}

Given.ENTITY_CLASS = {
  TASK_ID: TaskId,
  TASK: Task,
};

Given.TYPE = {
  TASK_ID: Type.forClass(TaskId),
  TASK: Type.forClass(Task),
};

describe('QueryBuilder', function () {

  const timeoutDuration = new Duration({seconds: 5});
  this.timeout(timeoutDuration.inMs());

  it('creates a Query of query for type', done => {
    const query = Given.requestFactory()
      .query()
      .select(Given.ENTITY_CLASS.TASK)
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
      .select(Given.ENTITY_CLASS.TASK)
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
      .select(Given.ENTITY_CLASS.TASK)
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

    const targetIds = idFilter.getIdList()
      .map(any => AnyPacker.unpack(any).as(Given.TYPE.TASK_ID))
      .map(taskId => taskId.getValue());

    Given.assertUnorderedEqual(targetIds, values);

    done();
  });

  it('creates a Query for type with string IDs', done => {
    const values = ['meeny', 'miny', 'moe'];

    const query = Given.requestFactory()
      .query()
      .select(Given.ENTITY_CLASS.TASK)
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

    const targetIds = idFilter.getIdList()
      .map(any => AnyPacker.unpack(any).asString());

    Given.assertUnorderedEqual(targetIds, values);

    done();
  });

  it('creates a Query for type with number IDs', done => {
    const values = [29, 99971, 104729];

    const query = Given.requestFactory()
      .query()
      .select(Given.ENTITY_CLASS.TASK)
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

    const targetIds = idFilter.getIdList()
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
        .select(Given.ENTITY_CLASS.TASK)
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
        .select(Given.ENTITY_CLASS.TASK)
        .byIds({error: true});
      done(new Error('#byIds() non-Array value did not result in error.'));
    } catch (error) {
      done();
    }
  });

  it('throws an error if #byIds() is invoked with non-TypedMessage IDs', done => {
    try {
      Given.requestFactory().query()
        .select(Given.ENTITY_CLASS.TASK)
        .byIds([{tinker: 'tailor'}, {soldier: 'sailor'}]);
      done(new Error('#byIds() non-TypedMessage IDs did not result in error.'));
    } catch (error) {
      done();
    }
  });

  /********* FILTERS *********/

  it('creates a Query with no filters', done => {
    const query = Given.requestFactory()
      .query()
      .select(Given.ENTITY_CLASS.TASK)
      .where([])
      .build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    done();
  });

  it('creates a Query with a single filter', done => {
    const nameFilter = Filters.eq('name', 'Implement tests');
    const query = Given.requestFactory()
      .query()
      .select(Given.ENTITY_CLASS.TASK)
      .where([nameFilter])
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(!target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const expectedFilters = new TargetFilters();
    expectedFilters.setFilterList([Filters.all([nameFilter])]);

    Given.assertMessagesEqual(target.getFilters(), expectedFilters);

    done();
  });

  it('creates a Query with a multiple filters', done => {
    const nameFilter = Filters.eq('name', 'Implement tests');
    const descriptionFilter = Filters.eq('description', 'Web needs tests, eh?');
    const query = Given.requestFactory()
      .query()
      .select(Given.ENTITY_CLASS.TASK)
      .where([nameFilter, descriptionFilter])
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(!target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const expectedFilters = new TargetFilters();
    expectedFilters.setFilterList([Filters.all([nameFilter, descriptionFilter])]);

    Given.assertMessagesEqual(target.getFilters(), expectedFilters);

    done();
  });

  it('creates a Query with a single CompositeFilter', done => {
    const nameFilter1 = Filters.eq('name', 'Implement tests');
    const nameFilter2 = Filters.eq('name', 'Create a PR');
    const compositeFilter = Filters.either([nameFilter1, nameFilter2]);
    const query = Given.requestFactory()
      .query()
      .select(Given.ENTITY_CLASS.TASK)
      .where([compositeFilter])
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(!target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const expectedFilters = new TargetFilters();
    expectedFilters.setFilterList([compositeFilter]);

    Given.assertMessagesEqual(target.getFilters(), expectedFilters);

    done();
  });

  it('creates a Query with a multiple CompositeFilters', done => {
    const nameFilter1 = Filters.eq('name', 'Implement tests');
    const nameFilter2 = Filters.eq('name', 'Create a PR');
    const nameFilter = Filters.either([nameFilter1, nameFilter2]);
    const descriptionFilter = Filters.all([
      Filters.eq('description', 'Web needs tests, eh?'),
    ]);

    const query = Given.requestFactory()
      .query()
      .select(Given.ENTITY_CLASS.TASK)
      .where([nameFilter, descriptionFilter])
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(!target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    const expectedFilters = new TargetFilters();
    expectedFilters.setFilterList([nameFilter, descriptionFilter]);

    Given.assertMessagesEqual(target.getFilters(), expectedFilters);

    done();
  });

  it('throws an error if #where() is invoked with non-Array value', done => {
    const nameFilter = Filters.eq('name', 'Implement tests');

    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.ENTITY_CLASS.TASK)
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
        .select(Given.ENTITY_CLASS.TASK)
        .where(['Duck', 'duck', 'goose']);
      done(new Error('An error was expected due to invalid #where() parameter.'));
    } catch (e) {
      done();
    }
  });

  it('throws an error if #where() is invoked with mixed Filter and CompositeFilter values', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.ENTITY_CLASS.TASK)
        .where([new Filter(), new CompositeFilter()]);
      done(new Error('An error was expected due to mixed filter types.'));
    } catch (e) {
      done();
    }
  });

  it('throws an error if #where() is invoked more than once', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.ENTITY_CLASS.TASK)
        .where([new Filter()])
        .where([new Filter()]);
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
      .select(Given.ENTITY_CLASS.TASK)
      .withMask(maskedFields)
      .build();

    assert.ok(query.getId());
    Given.assertActorContextCorrect(query.getContext());

    const target = query.getTarget();
    assert.ok(target);
    assert.ok(target.getIncludeAll());
    Given.assertTargetTypeEqual(target, Given.TYPE.TASK);

    Given.assertUnorderedEqual(query.getFormat().getFieldMask().getPathsList(), maskedFields);

    done();
  });

  it('throws an error if #withMask() is invoked more than once', done => {
    try {
      const query = Given.requestFactory()
        .query()
        .select(Given.ENTITY_CLASS.TASK)
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
        .select(Given.ENTITY_CLASS.TASK)
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
        .select(Given.ENTITY_CLASS.TASK)
        .withMask([22]);
      done(new Error('An error was expected due to invalid #withMask() argument.'))
    } catch (e) {
      done();
    }
  });

  /********* LIMIT *********/

  it('creates a Query with a limit and ascending ordering', done => {
    const limit = 42;
    const fieldName = 'when_created';
    const query = Given.requestFactory()
        .query()
        .select(Given.ENTITY_CLASS.TASK)
        .limit(limit)
        .orderAscendingBy(fieldName)
        .build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const format = query.getFormat();
    assert.strictEqual(limit, format.getLimit());

    let orderBy = Ordering.firstOrderBy(format.getOrderByList());
    assert.strictEqual(fieldName, orderBy.getColumn());
    assert.strictEqual(OrderBy.Direction.ASCENDING, orderBy.getDirection());

    done();
  });

  it('creates a Query with a limit and descending ordering', done => {
    const limit = 42;
    const fieldName = 'name';
    const query = Given.requestFactory()
        .query()
        .select(Given.ENTITY_CLASS.TASK)
        .limit(limit)
        .orderDescendingBy(fieldName)
        .build();

    assert.ok(query.getId());

    Given.assertActorContextCorrect(query.getContext());

    const format = query.getFormat();
    assert.strictEqual(limit, format.getLimit());

    let orderBy = Ordering.firstOrderBy(format.getOrderByList());
    assert.strictEqual(fieldName, orderBy.getColumn());
    assert.strictEqual(OrderBy.Direction.DESCENDING, orderBy.getDirection());

    done();
  });

  it('does not allow `limit` without `order_by`', done => {
    const builder = Given.requestFactory()
        .query()
        .select(Given.ENTITY_CLASS.TASK)
        .limit(42);
    assert.throws(() => builder.build(), Error);
    done();
  });
});
