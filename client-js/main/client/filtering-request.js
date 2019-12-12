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

import {ClientRequest} from "./client-request";

/**
 * An abstract base for client requests that filter messages by certain criteria.
 *
 * @abstract
 *
 * @template <B> the type of the builder wrapped by this request
 * @template <T> the type of the messages that store the request data
 */
export class FilteringRequest extends ClientRequest {

  /**
   * @param {!Class<? extends Message>} targetType the target type of the request
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
   *
   * @protected
   */
  constructor(targetType, client, actorRequestFactory) {
    super(client, actorRequestFactory);
    this.targetType = targetType;
  }

  /**
   * Adds filtering by IDs to the built request.
   *
   * @param ids {!<? extends Message>|Number|String|<? extends Message>[]|Number[]|String[]}
   *        the IDs of interest
   * @return {this} self for method chaining
   */
  byId(ids) {
    ids = FilteringRequest._ensureArray(ids);
    this._builder().byIds(ids);
    return this._self();
  }

  /**
   * Adds filtering by predicates to the built request.
   *
   * Filters specified in a list are considered to be joined using `AND` operator.
   *
   * @param {!Filter|CompositeFilter|Filter[]|CompositeFilter[]} predicates the filters
   * @return {this} self for method chaining
   */
  where(predicates) {
    predicates = FilteringRequest._ensureArray(predicates);
    this._builder().where(predicates);
    return this._self();
  }

  /**
   * Applies a field mask to the request results.
   *
   * The names of the fields must be formatted according to the `google.protobuf.FieldMask`
   * specification.
   *
   * @param {!String|String[]} fieldPaths the fields to include in the mask
   * @return {this} self for method chaining
   */
  withMask(fieldPaths) {
    fieldPaths = FilteringRequest._ensureArray(fieldPaths);
    this._builder().withMask(fieldPaths);
    return this._self();
  }

  /**
   * Returns the builder for messages that store request data.
   *
   * @return {AbstractTargetBuilder<T extends Message>} the builder instance
   *
   * @protected
   */
  _builder() {
    if (!this._builderInstance) {
      const newBuilderFn = this._newBuilderFn();
      this._builderInstance = newBuilderFn(this._requestFactory);
    }
    return this._builderInstance;
  }

  /**
   * Returns the function with which the {@link _builderInstance} can be created.
   *
   * @abstract
   * @return {Function<ActorRequestFactory, B extends AbstractTargetBuilder>}
   *
   * @protected
   */
  _newBuilderFn() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @abstract
   * @return {this}
   *
   * @protected
   */
  _self() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Wraps the passed argument into array if it's not an array already.
   *
   * The `null` values are converted into an empty array.
   *
   * @return {Array} the passed argument as an array
   *
   * @private
   */
  static _ensureArray(values) {
    if (!values) {
      return [];
    }
    if (!(values instanceof Array)) {
      return [values]
    }
    return values;
  }
}
