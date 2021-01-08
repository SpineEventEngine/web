/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import {OrderBy} from "../proto/spine/client/query_pb";
import {FilteringRequest} from "./filtering-request";

/**
 * A request to retrieve entities of the given type.
 *
 * Allows to post the query data to the Spine backend and receive the entity states as `Promise`.
 *
 * A usage example:
 * ```
 * const customers =
 *          client.select(Customer.class)
 *                .byId(westCoastCustomerIds())
 *                .withMask("name", "address", "email")
 *                .where([Filters.eq("type", "permanent"),
 *                       Filters.eq("discount_percent", 10),
 *                       Filters.eq("company_size", Company.Size.SMALL)])
 *                .orderBy("name", OrderBy.Direction.ASCENDING)
 *                .limit(20)
 *                .run(); // The returned type is `Promise<Customer[]>`.
 * ```
 *
 * All of the called filtering methods are optional. If none of them are specified, all entities
 * of type will be retrieved.
 *
 * @template <T> the query target type
 */
export class QueryRequest extends FilteringRequest {

  /**
   * @param {!Class<Message>} targetType the target type of entities
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
   */
  constructor(targetType, client, actorRequestFactory) {
    super(targetType, client, actorRequestFactory)
  }

  /**
   * Sets the sorting order for the retrieved results.
   *
   * @param {!String} column the column to order by
   * @param {!OrderBy.Direction} direction the ascending/descending direction
   * @return {this} self for method chaining
   */
  orderBy(column, direction) {
    if (direction === OrderBy.Direction.ASCENDING) {
      this._builder().orderAscendingBy(column);
    } else {
      this._builder().orderDescendingBy(column);
    }
    return this._self();
  }

  /**
   * Sets the maximum number of returned entities.
   *
   * Can only be used in conjunction with the {@link #orderBy} condition.
   *
   * @param {number} count the max number of response entities
   * @return {this} self for method chaining
   */
  limit(count) {
    this._builder().limit(count);
    return this._self();
  }

  /**
   * Builds a `Query` instance based on currently specified filters.
   *
   * @return {spine.client.Query} a `Query` instance
   */
  query() {
    return this._builder().build();
  }

  /**
   * Runs the query and obtains the results.
   *
   * @return {Promise<Message[]>} the asynchronously resolved query results
   */
  run() {
    const query = this.query();
    return this._client.read(query);
  }

  /**
   * @inheritDoc
   */
  _newBuilderFn() {
    return requestFactory => requestFactory.query().select(this.targetType);
  }

  /**
   * @inheritDoc
   */
  _self() {
    return this;
  }
}
