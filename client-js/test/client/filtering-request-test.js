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
import {Type} from "@lib/client/typed-message";
import {Task, TaskId} from '@testProto/spine/test/js/task_pb';
import {MockClient} from "./test-helpers";

export class Given {

  constructor() {
    throw new Error('A utility `Given` class cannot be instantiated.');
  }

  static targetType() {
    return Task;
  }

  static client() {
    return new MockClient();
  }

  static actorRequestFactory() {
    return new ActorRequestFactory(new ActorProvider());
  }
}

/**
 * Emulates an abstract test.
 *
 * To run, call the function inside the actual test with the necessary callbacks provided.
 *
 * @param newRequest a callback which accepts a target type, client and request factory and creates
 *                   a new filtering request
 * @param buildResult a callback which accepts a filtering request and builds the message
 *                    that is sent to the Spine server (e.g. `Query`, `Topic`)
 * @param getTarget a callback which extracts the target from the result message
 * @param getFieldMask a callback which extracts the field mask from the result message
 */
export function filteringRequestTest(newRequest, buildResult, getTarget, getFieldMask) {

  const targetType = Given.targetType();
  const client = Given.client();
  const actorRequestFactory = Given.actorRequestFactory();

  const targetTypeUrl = Type.forClass(targetType).url().value();

  let request;

  beforeEach(done => {
    request = newRequest(targetType, client, actorRequestFactory);
    done();
  });

  it('creates a `select all` target', done => {
    const result = buildResult(request);
    const target = getTarget(result);
    assertIsIncludeAll(target);
    done();
  });

  it('creates a target filtering entities by a single ID', done => {
    const taskId = new TaskId();
    const idValue = uuid.v4();
    taskId.setValue(idValue);
    request.byId(taskId);

    const result = buildResult(request);
    const target = getTarget(result);
    assertTargetTypeEquals(target);
    const idFilter = target.getFilters().getIdFilter();
    const idList = idFilter.getIdList();
    const length = idList.length;
    assert.equal(
        1, length,
        `Expected the ID list to contain a single ID, the actual length: '${length}'.`
    );
    const taskIdType = Type.forClass(TaskId);
    const targetId = AnyPacker.unpack(idList[0]).as(taskIdType);
    const actualId = targetId.getValue();
    assert.equal(
        idValue, actualId,
        `Unexpected target ID '${actualId}', expected: '${idValue}'.`
    );
    done();
  });

  it('creates a target filtering entities by a group of IDs', done => {
    const taskId1 = new TaskId();
    const idValue1 = uuid.v4();
    taskId1.setValue(idValue1);
    const taskId2 = new TaskId();
    const idValue2 = uuid.v4();
    taskId2.setValue(idValue2);

    request.byId([taskId1, taskId2]);
    const result = buildResult(request);
    const target = getTarget(result);
    assertTargetTypeEquals(target);
    const idFilter = target.getFilters().getIdFilter();
    const idList = idFilter.getIdList();
    const length = idList.length;
    assert.equal(
        2, length,
        `Expected the ID list to contain two IDs, the actual length: '${length}'.`
    );
    done();
  });

  it('ignores a `null` ID specified to the `byId` method', done => {
    request.byId(null);
    const result = buildResult(request);
    const target = getTarget(result);
    assertIsIncludeAll(target);
    done();
  });

  it('ignores an empty array of IDs specified to the `byId` method', done => {
    request.byId([]);
    const result = buildResult(request);
    const target = getTarget(result);
    assertIsIncludeAll(target);
    done();
  });

  it('creates a target filtering entities by a single filter', done => {
    const filter = Filters.eq('name', 'some task name');
    request.where(filter);
    const result = buildResult(request);
    const target = getTarget(result);
    const compositeFilters = target.getFilters().getFilterList();
    const compositeFiltersLength = compositeFilters.length;
    assert.equal(
        1, compositeFiltersLength,
        `Expected the composite filter list to contain a single filter, the actual length: 
        '${compositeFiltersLength}'.`
    );
    const filters = compositeFilters[0].getFilterList();
    const length = filters.length;
    assert.equal(
        1, length,
        `Expected the filter list to contain a single filter, the actual length: '${length}'.`
    );
    const targetFilter = filters[0];
    assert.equal(
        filter, targetFilter,
        `Unexpected filter value '${targetFilter}', expected: '${filter}'.`
    );
    done();
  });

  it('creates a target filtering entities by a group of filters', done => {
    const filter1 = Filters.eq('name', 'some task name');
    const filter2 = Filters.eq('description', 'some task description');
    request.where([filter1, filter2]);
    const result = buildResult(request);
    const target = getTarget(result);
    const compositeFilters = target.getFilters().getFilterList();
    const compositeFiltersLength = compositeFilters.length;
    assert.equal(
        1, compositeFiltersLength,
        `Expected the composite filter list to contain a single filter, the actual 
        length: '${compositeFiltersLength}'.`
    );
    const filters = compositeFilters[0].getFilterList();
    const length = filters.length;
    assert.equal(
        2, length,
        `Expected the filter list to contain two filters, the actual length: '${length}'.`
    );
    done();
  });

  it('ignores a `null` filter specified to the `where` method', done => {
    request.where(null);
    const result = buildResult(request);
    const target = getTarget(result);
    assertIsIncludeAll(target);
    done();
  });

  it('ignores an empty filter list specified to the `where` method', done => {
    request.where([]);
    const result = buildResult(request);
    const target = getTarget(result);
    assertIsIncludeAll(target);
    done();
  });

  it('allows to specify a field mask', done => {
    const fields = ['id', 'name', 'description'];
    request.withMask(fields);
    const result = buildResult(request);
    const fieldMask = getFieldMask(result);
    const pathList = fieldMask.getPathsList();
    assert.equal(
        fields, pathList,
        `Unexpected list of fields in the field mask: '${pathList}', expected: '${fields}'.`
    );
    done();
  });

  /**
   * @param {!spine.client.Target} target
   */
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
        `The unexpected target type '${actualType}', expected: '${targetTypeUrl}'.`
    );
  }
}
