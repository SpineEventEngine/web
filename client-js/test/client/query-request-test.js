/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import {ActorProvider, ActorRequestFactory, Filters} from '@lib/client/actor-request-factory';
import {AnyPacker} from '@lib/client/any-packer';
import {QueryRequest} from "@lib/client/client-request";
import {Duration} from "@lib/client/time-utils";
import {Type} from "@lib/client/typed-message";
import {OrderBy} from '@proto/spine/client/query_pb';
import {Task, TaskId} from '@testProto/spine/test/js/task_pb';
import {MockClient} from "./test-helpers";

describe('QueryRequest', function () {

  const timeoutDuration = new Duration({seconds: 5});
  this.timeout(timeoutDuration.inMs());

  const targetType = Task;
  const clientStub = new MockClient();
  const actorRequestFactory = new ActorRequestFactory(new ActorProvider());

  const targetTypeUrl = Type.forClass(targetType).url().value();

  let request;

  beforeEach(done => {
    request = new QueryRequest(targetType, clientStub, actorRequestFactory);
    done();
  });

  it('creates a `select all` query', done => {
    const query = request.query();
    const target = query.getTarget();
    assertIsIncludeAll(target);
    done();
  });

  it('creates a query filtering entities by a single ID', done => {
    const taskId = new TaskId();
    const idValue = uuid.v4();
    taskId.setValue(idValue);

    const query = request.byId(taskId).query();
    const target = query.getTarget();
    assertTargetTypeEquals(target);
    const idFilter = target.getFilters().getIdFilter();
    const idList = idFilter.getIdList();
    const length = idList.length;
    assert.equal(
        1, length,
        `Expected the ID list to contain a single ID, the actual length: ${length}.`
    );
    const taskIdType = Type.forClass(TaskId);
    const targetId = AnyPacker.unpack(idList[0]).as(taskIdType);
    const actualId = targetId.getValue();
    assert.equal(
        idValue, actualId,
        `Unexpected target ID ${actualId}, expected: ${idValue}.`
    );
    done();
  });

  it('creates a query filtering entities by a group of IDs', done => {
    const taskId1 = new TaskId();
    const idValue1 = uuid.v4();
    taskId1.setValue(idValue1);
    const taskId2 = new TaskId();
    const idValue2 = uuid.v4();
    taskId2.setValue(idValue2);

    const query = request.byId([taskId1, taskId2]).query();
    const target = query.getTarget();
    assertTargetTypeEquals(target);
    const idFilter = target.getFilters().getIdFilter();
    const idList = idFilter.getIdList();
    const length = idList.length;
    assert.equal(
        2, length,
        `Expected the ID list to contain two IDs, the actual length: ${length}.`
    );
    done();
  });

  it('ignores a `null` ID specified to the `byId` method', done => {
    const query = request.byId(null).query();
    const target = query.getTarget();
    assertIsIncludeAll(target);
    done();
  });

  it('ignores an empty array of IDs specified to the `byId` method', done => {
    const query = request.byId([]).query();
    const target = query.getTarget();
    assertIsIncludeAll(target);
    done();
  });

  it('creates a query filtering entities by a single filter', done => {
    const filter = Filters.eq('name', 'some task name');
    const query = request.where(filter).query();
    const target = query.getTarget();
    const compositeFilters = target.getFilters().getFilterList();
    const compositeFiltersLength = compositeFilters.length;
    assert.equal(
        1, compositeFiltersLength,
        `Expected the composite filter list to contain a single filter, the actual 
        length: ${compositeFiltersLength}.`
    );
    const filters = compositeFilters[0].getFilterList();
    const length = filters.length;
    assert.equal(
        1, length,
        `Expected the filter list to contain a single filter, the actual length: ${length}.`
    );
    const targetFilter = filters[0];
    assert.equal(
      filter, targetFilter,
      `Unexpected filter value ${targetFilter}, expected: ${filter}.`
    );
    done();
  });

  it('creates a query filtering entities by a group of filters', done => {
    const filter1 = Filters.eq('name', 'some task name');
    const filter2 = Filters.eq('description', 'some task description');
    const query = request.where([filter1, filter2]).query();
    const target = query.getTarget();
    const compositeFilters = target.getFilters().getFilterList();
    const compositeFiltersLength = compositeFilters.length;
    assert.equal(
        1, compositeFiltersLength,
        `Expected the composite filter list to contain a single filter, the actual 
        length: ${compositeFiltersLength}.`
    );
    const filters = compositeFilters[0].getFilterList();
    const length = filters.length;
    assert.equal(
        2, length,
        `Expected the filter list to contain two filters, the actual length: ${length}.`
    );
    done();
  });

  it('ignores a `null` filter specified to the `where` method', done => {
    const query = request.where(null).query();
    const target = query.getTarget();
    assertIsIncludeAll(target);
    done();
  });

  it('ignores an empty filter list specified to the `where` method', done => {
    const query = request.where([]).query();
    const target = query.getTarget();
    assertIsIncludeAll(target);
    done();
  });

  it('allows to set a field mask', done => {
    const fields = ['id', 'name', 'description'];
    const query = request.withMask(fields).query();
    const responseFormat = query.getFormat();
    const fieldMask = responseFormat.getFieldMask();
    const pathList = fieldMask.getPathsList();
    assert.equal(
      fields, pathList,
      `Unexpected list of fields in the field mask: ${pathList}, expected: ${fields}.`
    );
    done();
  });

  it('allows to set ordering and limit', done => {
    const column = 'name';
    const direction = OrderBy.Direction.ASCENDING;
    const query = request.orderBy(column, direction)
        .limit(2)
        .query();
    const responseFormat = query.getFormat();
    const orderBy = responseFormat.getOrderBy();
    const orderByColumn = orderBy.getColumn();
    assert.equal(
        column, orderByColumn,
        `Unexpected column specified in the order by: ${orderByColumn}, expected: ${column}.`
    );
    const orderByDirection = orderBy.getDirection();
    assert.equal(
        direction, orderByDirection,
        `Unexpected direction specified in the order by: ${orderByDirection}, 
        expected: ${direction}.`
    );
    done();
  });

  function assertIsIncludeAll(target) {
    assertTargetTypeEquals(target);
    assert.equal(
        true, target.getIncludeAll(),
        'Expected `target.include_all` to be `true`.'
    );
  }

  /**
   * @param {!spine.client.Target} target
   */
  function assertTargetTypeEquals(target) {
    const actualType = target.getType();
    assert.equal(
        targetTypeUrl, actualType,
        `The unexpected target type ${actualType}, expected: ${targetTypeUrl}.`
    );
  }
});
