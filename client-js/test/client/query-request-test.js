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
import {QueryRequest} from "@lib/client/query-request";
import {Duration} from "@lib/client/time-utils";
import {OrderBy} from '@proto/spine/client/query_pb';
import {filteringRequestTest, Given} from "./filtering-request-test";

describe('QueryRequest', function () {

  const timeoutDuration = new Duration({seconds: 5});
  this.timeout(timeoutDuration.inMs());

  filteringRequestTest(newQueryRequest, buildQuery, getTarget, getFieldMask);

  it('allows to set ordering and limit', done => {
    const request =
        newQueryRequest(Given.targetType(), Given.client(), Given.actorRequestFactory());
    const column = 'name';
    const direction = OrderBy.Direction.ASCENDING;
    const query = request.orderBy(column, direction)
        .limit(2)
        .query();
    const responseFormat = query.getFormat();
    const orderBy = responseFormat.getOrderBy();
    const orderByColumn = orderBy.getColumn();
    assert.strictEqual(
        column, orderByColumn,
        `Unexpected column specified in the order by: '${orderByColumn}', expected: '${column}'.`
    );
    const orderByDirection = orderBy.getDirection();
    assert.strictEqual(
        direction, orderByDirection,
        `Unexpected direction specified in the order by: '${orderByDirection}',
        expected: '${direction}'.`
    );
    done();
  });

  function newQueryRequest(targetType, clientStub, actorRequestFactory) {
    return new QueryRequest(targetType, clientStub, actorRequestFactory);
  }

  function buildQuery(request) {
    return request.query();
  }

  function getTarget(query) {
    return query.getTarget();
  }

  function getFieldMask(query) {
    return query.getFormat().getFieldMask();
  }
});
