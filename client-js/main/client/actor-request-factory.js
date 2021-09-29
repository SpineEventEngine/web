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

"use strict";

import {v4 as newUuid} from 'uuid';

import {Message} from 'google-protobuf';
import {FieldMask} from '../proto/google/protobuf/field_mask_pb';
import {Timestamp} from '../proto/google/protobuf/timestamp_pb';
import {
  CompositeFilter,
  Filter,
  IdFilter,
  Target,
  TargetFilters
} from '../proto/spine/client/filters_pb';
import {OrderBy, Query, QueryId, ResponseFormat} from '../proto/spine/client/query_pb';
import {Topic, TopicId} from '../proto/spine/client/subscription_pb';
import {ActorContext} from '../proto/spine/core/actor_context_pb';
import {Command, CommandContext, CommandId} from '../proto/spine/core/command_pb';
import {UserId} from '../proto/spine/core/user_id_pb';
import {ZoneId, ZoneOffset} from '../proto/spine/time/time_pb';
import {isProtobufMessage, Type, TypedMessage} from './typed-message';
import {AnyPacker} from './any-packer';
import {FieldPaths} from './field-paths';
import {EnumValue} from 'google-protobuf/google/protobuf/type_pb';

/**
 * Wraps the passed enum value as Protobuf `EnumValue` so it can be correctly processed by the
 * server.
 *
 * As enums in Protobuf JS are declared as plain `number`s, their values can be passed to this
 * method as-is, for example: `enumValueOf(Task.Severity.HIGH)`.
 *
 * @param {!number} value the enum value
 * @returns {EnumValue} the `EnumValue` instance
 */
export function enumValueOf(value) {
  const result = new EnumValue();
  result.setNumber(value);
  return result;
}

const ENUM_VALUE_TYPE_URL = 'type.googleapis.com/google.protobuf.EnumValue';

// TODO:2019-06-07:yegor.udovchenko: Cover `Filters` class with the unit tests
// https://github.com/SpineEventEngine/web/issues/100
/**
 * A factory for `Filter` and `CompositeFilter` instances.
 */
export class Filters {

  /**
   * @typedef {string | number | boolean | Date | TypedMessage<T> | Message} FieldValue
   *
   * Represents all types acceptable as a value for filtering.
   *
   * @template <T> a type of the Protobuf message to compare with
   */

  /**
   * Instantiation not allowed and will throw an error.
   */
  constructor() {
    throw new Error('Tried instantiating a utility class.');
  }

  /**
   * Creates a new filter for the value of an object field to be equal to the provided value.
   *
   * @param {!String} fieldPath a path to the object field
   * @param {!FieldValue} value a value to compare with
   *
   * @return {Filter} a new filter instance
   */
  static eq(fieldPath, value) {
    return Filters.with(fieldPath, Filter.Operator.EQUAL, value);
  }

  /**
   * Creates a new filter for the value of an object field to be less than the provided value.
   *
   * @param {!String} fieldPath a path to the object field
   * @param {!FieldValue} value a value to compare with
   *
   * @return {Filter} a new filter instance
   */
  static lt(fieldPath, value) {
    return Filters.with(fieldPath, Filter.Operator.LESS_THAN, value);
  }

  /**
   * Creates a new filter for the value an object field to be greater than the provided value.
   *
   * @param {!String} fieldPath a path to the object field
   * @param {!FieldValue} value a value to compare with
   *
   * @return {Filter} a new filter instance
   */
  static gt(fieldPath, value) {
    return Filters.with(fieldPath, Filter.Operator.GREATER_THAN, value);
  }

  /**
   * Creates a new filter for the value of an object field to be less or equal compared to
   * the provided value.
   *
   * @param {!String} fieldPath a path to the object field
   * @param {!FieldValue} value a value to compare with
   *
   * @return {Filter} a new filter instance
   */
  static le(fieldPath, value) {
    return Filters.with(fieldPath, Filter.Operator.LESS_OR_EQUAL, value);
  }

  /**
   * Creates a new filter for the value of an object field to be greater or equal compared to
   * the provided value.
   *
   * @param {!String} fieldPath a path to the object field
   * @param {!FieldValue} value a value to compare with
   *
   * @return {Filter} a new filter instance
   */
  static ge(fieldPath, value) {
    return Filters.with(fieldPath, Filter.Operator.GREATER_OR_EQUAL, value);
  }

  /**
   * Creates a filter for an object field to match the provided value according to an operator.
   *
   * Accepts various types of {@link FieldValue field values}.
   *
   * @example
   * // Create filters with primitive values to compare
   * Filters.eq('description', 'Sample task description') // Wraps string in the Protobuf `StringValue`
   * Filters.gt('length', 12) // Wraps number in the Protobuf `Int32Value`
   * Filters.eq('multiline', false) // Wraps boolean in the Protobuf `BoolValue`
   *
   * @example
   * // Create filter for the primitive value of a custom type
   * Filters.gt('price', TypedMessage.float(7.41))
   *
   * @example
   * // Create filter for the time-based value
   * Filters.gt('whenCreated', new Date(2019, 5, 4)) // Converts the given date to the `Timestamp` message
   *
   * @example
   * // Create filter for the user-defined type
   * Filters.eq('status', Task.Status.COMPLETED)
   *
   * @param {!String} fieldPath a path to the object field
   * @param {!Filter.Operator} operator an operator to check the field value upon
   * @param {!FieldValue} value a value to compare the field value to
   *
   * @return {Filter} a new filter instance
   */
  static with(fieldPath, operator, value) {
    let typedValue;

    if (value instanceof Number || typeof value === 'number') {
      typedValue = TypedMessage.int32(value);
    } else if (value instanceof String || typeof value === 'string') {
      typedValue = TypedMessage.string(value);
    } else if (value instanceof Boolean || typeof value === 'boolean') {
      typedValue = TypedMessage.bool(value);
    } else if (value instanceof Date) {
      typedValue = TypedMessage.timestamp(value);
    } else if (value instanceof EnumValue) {
      const type = Type.of(EnumValue, ENUM_VALUE_TYPE_URL);
      typedValue = new TypedMessage(value, type);
    } else if (value instanceof TypedMessage) {
      typedValue = value;
    } else if(isProtobufMessage(value)) {
      typedValue = TypedMessage.of(value);
    } else {
      throw new Error(`Unable to create filter.
       Filter value type of ${typeof value} is unsupported.`)
    }

    const wrappedValue = AnyPacker.packTyped(typedValue);
    const filter = new Filter();
    filter.setFieldPath(FieldPaths.parse(fieldPath));
    filter.setValue(wrappedValue);
    filter.setOperator(operator);
    return filter;
  }

  /**
   * Creates a new composite filter which matches objects that fit every provided filter.
   *
   * @param {!Filter[]} filters an array of simple filters
   *
   * @return {CompositeFilter} a new composite filter with `ALL` operator
   */
  static all(filters) {
    return Filters.compose(filters, CompositeFilter.CompositeOperator.ALL);
  }

  /**
   * Creates a new composite filter which matches objects that fit at least one
   * of the provided filters.
   *
   * @param {!Filter[]} filters an array of simple filters
   *
   * @return {CompositeFilter} a new composite filter with `EITHER` operator
   */
  static either(filters) {
    return Filters.compose(filters, CompositeFilter.CompositeOperator.EITHER);
  }

  /**
   * Creates a new composite filter which matches objects according to an array of filters with a
   * specified logical operator.
   *
   * @param {!Filter[]} filters an array of simple filters
   * @param {!CompositeFilter.CompositeOperator} operator a logical operator for `filters`
   *
   * @return {CompositeFilter} a new composite filter
   */
  static compose(filters, operator) {
    const compositeFilter = new CompositeFilter();
    compositeFilter.setFilterList(filters);
    compositeFilter.setOperator(operator);
    return compositeFilter;
  }
}

/**
 * Utilities for working with `Query` and `Topic` targets.
 */
class Targets {

  /**
   * Instantiation not allowed and will throw an error.
   */
  constructor() {
    throw new Error('Tried instantiating a utility class.');
  }

  /**
   * Composes a new target for objects of specified type, optionally with specified IDs and
   * filters.
   *
   * @param {!Type} type a Type URL of target objects
   * @param {?TypedMessage[]} ids an array of IDs one of which must be matched by each target
   *                              object
   * @param {?CompositeFilter[]} filters an array of filters target
   *
   * @return {Target} a newly created target for objects matching the specified filters
   */
  static compose({forType: type, withIds: ids, filteredBy: filters}) {
    const includeAll = !ids && !filters;

    if (includeAll) {
      return Targets._all(type);
    }

    const targetFilters = new TargetFilters();

    const idList = Targets._nullToEmpty(ids);
    if (idList.length) {
      const idFilter = Targets._assembleIdFilter(idList);
      targetFilters.setIdFilter(idFilter);
    }

    const filterList = Targets._nullToEmpty(filters);
    if (filterList) {
      targetFilters.setFilterList(filterList);
    }

    return Targets._filtered(type, targetFilters);
  }

  /**
   * Creates a new target including all items of type.
   *
   * @param {!Type} type
   * @return {Target}
   * @private
   */
  static _all(type) {
    const target = new Target();
    target.setType(type.url().value());
    target.setIncludeAll(true);
    return target;
  }

  /**
   * Creates a new target including only items of the specified type that pass filtering.
   *
   * @param {!Type} type
   * @param {!TargetFilters} filters
   * @return {Target}
   * @private
   */
  static _filtered(type, filters) {
    const target = new Target();
    target.setType(type.url().value());
    target.setFilters(filters);
    return target;
  }

  /**
   * Creates a targets ID filter including only items which are included in the provided ID list.
   *
   * @param {!TypedMessage[]} ids an array of IDs for items matching target to be included in
   * @return {IdFilter}
   * @private
   */
  static _assembleIdFilter(ids) {
    const idFilter = new IdFilter();
    ids.forEach(rawId => {
      const packedId = AnyPacker.packTyped(rawId);
      idFilter.addId(packedId);
    });
    return idFilter;
  }

  /**
   * @param {?T[]} input
   * @return {T[]} an empty array if the value is `null`, or the provided input otherwise
   * @template <T> type of items in the provided array
   * @private
   */
  static _nullToEmpty(input) {
    if (input == null) {
      return [];
    } else {
      return input;
    }
  }
}

const INVALID_FILTER_TYPE =
  'All filters passed to QueryFilter#where() must be of a single type: ' +
  'either Filter or CompositeFilter.';

/**
 * An abstract base for builders that create `Message` instances which have a `Target`
 * and a `FieldMask` as attributes.
 *
 * <p>The `Target` matching the builder configuration is accessed with `#getTarget()`,
 * while the `FieldMask` is retrieved with `#getMask()`.
 *
 * The public API of this class is inspired by the SQL syntax.
 * ```javascript
 *     select(CUSTOMER_TYPE) // returning <AbstractTargetBuilder> descendant instance
 *         .byIds(getWestCoastCustomerIds())
 *         .withMask(["name", "address", "email"])
 *         .where([
 *             Filters.eq("type", "permanent"),
 *             Filters.eq("discountPercent", 10),
 *             Filters.eq("companySize", Company.Size.SMALL)
 *         ])
 *         .build()
 * ```
 *
 * @template <T>
 *         a type of the message which is returned by the implementations `#build()`
 * @abstract
 */
class AbstractTargetBuilder {

  /**
   * @param {!Class<Message>} entity a Protobuf type of the target entities
   */
  constructor(entity) {
    /**
     * A type composed from the target entity class.
     * 
     * @type {Type}
     * @private
     */
    this._type = Type.forClass(entity);
    /**
     * @type {TypedMessage[]}
     * @private
     */
    this._ids = null;
    /**
     * @type {CompositeFilter[]}
     * @private
     */
    this._filters = null;
    /**
     * @type {FieldMask}
     * @private
     */
    this._fieldMask = null;
  }

  /**
   * Sets an ID predicate of the `Query#getTarget()`.
   *
   * Makes the query return only the items identified by the provided IDs.
   *
   * Supported ID types are string, number, and Protobuf messages. All of the passed
   * IDs must be of the same type.
   *
   * If number IDs are passed they are assumed to be of `int64` Protobuf type.
   *
   * @param {!Message[]|!Number[]|!String[]} ids an array with identifiers to query
   * @return {this} the current builder instance
   * @throws if this method is executed more than once
   * @throws if the provided IDs are not an instance of `Array`
   * @throws if any of provided IDs are not an instance of supported types
   * @throws if the provided IDs are not of the same type
   */
  byIds(ids) {
    if (this._ids !== null) {
      throw new Error('Can not set query ID more than once for QueryBuilder.');
    }
    if (!(ids instanceof Array)) {
      throw new Error('Only an array of IDs is allowed as parameter to QueryBuilder#byIds().');
    }
    if (!ids.length) {
      return this;
    }
    const invalidTypeMessage = 'Each provided ID must be a string, number or a Protobuf message.';
    if (ids[0] instanceof Number || typeof ids[0] === 'number') {
      AbstractTargetBuilder._checkAllOfType(ids, Number, invalidTypeMessage);
      this._ids = ids.map(TypedMessage.int64);
    } else if (ids[0] instanceof String || typeof ids[0] === 'string') {
      AbstractTargetBuilder._checkAllOfType(ids, String, invalidTypeMessage);
      this._ids = ids.map(TypedMessage.string);
    } else if (!isProtobufMessage(ids[0])){
      throw new Error(invalidTypeMessage);
    } else {
      AbstractTargetBuilder._checkAllOfType(ids, ids[0].constructor, invalidTypeMessage);
      this._ids = ids.map(id => TypedMessage.of(id));
    }
    return this;
  }

  /**
   * Sets a field value predicate of the `Query#getTarget()`.
   *
   * <p>If there are no `Filter`s (i.e. the provided array is empty), all
   * the records will be returned by executing the `Query`.
   *
   * <p>An array of predicates provided to this method are considered to be joined in
   * a conjunction (using `CompositeFilter.CompositeOperator#ALL`). This means
   * a record would match this query only if it matches all of the predicates.
   *
   * @param {!Filter[]|CompositeFilter[]} predicates
   * the predicates to filter the requested items by
   * @return {this} self for method chaining
   * @throws if this method is executed more than once
   * @see Filters a convenient way to create `Filter` instances
   */
  where(predicates) {
    if (this._filters !== null) {
      throw new Error('Can not set filters more than once for QueryBuilder.');
    }
    if (!(predicates instanceof Array)) {
      throw new Error('Only an array of predicates is allowed as parameter to QueryBuilder#where().');
    }
    if (!predicates.length) {
      return this;
    }
    if (predicates[0] instanceof Filter) {
      AbstractTargetBuilder._checkAllOfType(predicates, Filter, INVALID_FILTER_TYPE);
      const aggregatingFilter = Filters.all(predicates);
      this._filters = [aggregatingFilter];
    } else {
      AbstractTargetBuilder._checkAllOfType(predicates, CompositeFilter, INVALID_FILTER_TYPE);
      this._filters = predicates.slice();
    }
    return this;
  }

  /**
   * Sets a Field Mask of the `Query`.
   *
   * The names of the fields must be formatted according to the `FieldMask`
   * specification.
   *
   * If there are no fields (i.e. an empty array is passed), all the fields will
   * be returned by query.
   *
   * @param {!String[]} fieldNames
   * @return {this} self for method chaining
   * @throws if this method is executed more than once
   * @see FieldMask specification for `FieldMask`
   */
  withMask(fieldNames) {
    if (this._fieldMask != null) {
      throw new Error('Can not set field mask more than once for QueryBuilder.');
    }
    if (!(fieldNames instanceof Array)) {
      throw new Error('Only an array of strings is allowed as parameter to QueryBuilder#withMask().');
    }
    AbstractTargetBuilder._checkAllOfType(fieldNames, String, 'Field names should be strings.');
    if (!fieldNames.length) {
      return this;
    }
    this._fieldMask = new FieldMask();
    this._fieldMask.setPathsList(fieldNames);
    return this;
  }

  /**
   * @return {Target} a target matching builders configuration
   */
  getTarget() {
    return this._buildTarget();
  }

  /**
   * Creates a new target `Target` instance based on this builder configuration.
   *
   * @return {Target} a new target
   */
  _buildTarget() {
    return Targets.compose({forType: this._type, withIds: this._ids, filteredBy: this._filters});
  }

  /**
   * @return {FieldMask} a fields mask set to this builder
   */
  getMask() {
    return this._fieldMask;
  }

  /**
   * A build method for creating instances of this builders target class.
   *
   * @return {T} a new target class instance
   * @abstract
   */
  build() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Checks that each provided item is an instance of the provided class. In case the check does
   * not pass an error is thrown.
   *
   * @param {!Array} items an array of objects that are expected to be of the provided type
   * @param {!Object} cls a class each item is required to be instance of
   * @param {!String} message an error message thrown on type mismatch
   * @private
   */
  static _checkAllOfType(items, cls, message = 'Unexpected parameter type.') {
    if (cls === String) {
      AbstractTargetBuilder._checkAllAreStrings(items, message);
    } else if (cls === Number) {
      AbstractTargetBuilder._checkAllAreNumbers(items, message);
    } else if (cls === Boolean) {
      AbstractTargetBuilder._checkAllAreBooleans(items, message);
    } else {
      AbstractTargetBuilder._checkAllOfClass(cls, items, message);
    }
  }

  /**
   * @param {!Array} items an array of objects that are expected to be strings
   * @param {!String} message an error message thrown on type mismatch
   * @private
   */
  static _checkAllAreStrings(items, message) {
    items.forEach(item => {
      if (typeof item !== 'string' && !(item instanceof String)) {
        throw new Error(message);
      }
    });
  }

  /**
   * @param {!Array} items an array of objects that are expected to be numbers
   * @param {!String} message an error message thrown on type mismatch
   * @private
   */
  static _checkAllAreNumbers(items, message) {
    items.forEach(item => {
      if (typeof item !== 'number' && !(item instanceof Number)) {
        throw new Error(message);
      }
    });
  }

  /**
   * @param {!Array} items an array of objects that are expected to be booleans
   * @param {!String} message an error message thrown on type mismatch
   * @private
   */
  static _checkAllAreBooleans(items, message) {
    items.forEach(item => {
      if (typeof item !== 'boolean' && !(item instanceof Boolean)) {
        throw new Error(message);
      }
    });
  }

  /**
   * @param {!Object} cls a class tyo check items against
   * @param {!Array} items an array of objects that are expected to instances of class
   * @param {!String} message an error message thrown on type mismatch
   * @private
   */
  static _checkAllOfClass(cls, items, message) {
    items.forEach(item => {
      if (!(item instanceof cls)) {
        throw new Error(message);
      }
    });
  }
}

/**
 * A builder for creating `Query` instances. A more flexible approach to query creation
 * than using a `QueryFactory`.
 *
 * @extends {AbstractTargetBuilder<Query>}
 * @template <T> a Protobuf type of the query target entities
 */
class QueryBuilder extends AbstractTargetBuilder {

  /**
   * @param {!Class<Message>} entity a Protobuf type of the query target entities
   * @param {!QueryFactory} queryFactory
   */
  constructor(entity, queryFactory) {
    super(entity);
    /**
     * @type {QueryFactory}
     * @private
     */
    this._factory = queryFactory;

    /**
     * @type {number}
     * @private
     */
    this._limit = 0;

    /**
     * @type {OrderBy}
     * @private
     */
    this._orderBy = null;
  }

  /**
   * Limits the query response to the given number of entities.
   *
   * The value must be non-negative, otherwise an error occurs. If set to `0`, all the available
   * entities are retrieved.
   *
   * When set, the result ordering must also be specified.
   *
   * @param {number} limit the max number of response entities
   */
  limit(limit) {
    if (limit < 0) {
      throw new Error("Query limit must not be negative.");
    }
    this._limit = limit;
    return this;
  }

  /**
   * Requests the query results to be ordered by the given `column` in the descending direction.
   *
   * Whether the results will be sorted in the requested order depends on the implementation of
   * server-side communication. For example, the Firebase-based communication protocol does not
   * preserve ordering. Regardless, if a `limit` is set for a query, an ordering is also required.
   *
   * @param column
   */
  orderDescendingBy(column) {
    this._addOrderBy(column, OrderBy.Direction.DESCENDING);
    return this;
  }

  /**
   * Requests the query results to be ordered by the given `column` in the ascending direction.
   *
   * Whether the results will be sorted in the requested order depends on the implementation of
   * server-side communication. For example, the Firebase-based communication protocol does not
   * preserve ordering. Regardless, if a `limit` is set for a query, an ordering is also required.
   *
   * @param column
   */
  orderAscendingBy(column) {
    this._addOrderBy(column, OrderBy.Direction.ASCENDING);
    return this;
  }

  /**
   * Specifies the expected response ordering.
   *
   * @param column the name of the column to order by
   * @param direction the direction of ordering: `OrderBy.Direction.ASCENDING` or
   *        `OrderBy.Direction.DESCENDING`
   * @private
   */
  _addOrderBy(column, direction) {
    if (column === null) {
      throw new Error("Column name must not be `null`.");
    }
    this._orderBy = new OrderBy();
    this._orderBy.setColumn(column);
    this._orderBy.setDirection(direction);
  }

  /**
   * Creates the Query instance based on the current builder configuration.
   *
   * @return {Query} a new query
   */
  build() {
    const target = this.getTarget();
    const fieldMask = this.getMask();
    const limit = this._limit;
    const order = this._orderBy;
    if (limit !== 0 && order === null) {
        throw Error("Ordering is required for queries with a `limit`.")
    }
    return this._factory.compose({forTarget: target,
                                  withMask: fieldMask,
                                  limit: limit,
                                  orderBy: order});
  }
}

/**
 * A factory for creating `Query` instances specifying the data to be retrieved from Spine server.
 *
 * @see ActorRequestFactory#query()
 * @template <T> a Protobuf type of the query target entities
 */
class QueryFactory {

  /**
   * @param {!ActorRequestFactory} requestFactory
   */
  constructor(requestFactory) {
    this._requestFactory = requestFactory;
  }

  /**
   * Creates a new builder of `Query` instances of the provided type.
   *
   * @param {!Class<Message>} entity a Protobuf type of the query target entities
   * @return {QueryBuilder}
   */
  select(entity) {
    return new QueryBuilder(entity, this);
  }

  /**
   * Creates a new `Query` which would return only entities which conform the target specification.
   *
   * An optional field mask can be provided to specify particular fields to be returned for `Query`
   *
   * @param {!Target} forTarget a specification of type and filters for `Query` result to match
   * @param {?FieldMask} fieldMask a specification of fields to be returned by executing `Query`
   * @param {number} limit max number of entities to fetch
   * @return {Query}
   */
  compose({forTarget: target, withMask: fieldMask, limit: limit, orderBy: orderBy}) {
    return this._newQuery(target, fieldMask, limit, orderBy);
  }

  /**
   * @param {!Target} target a specification of type and filters for `Query` result to match
   * @param {?FieldMask} fieldMask a specification of fields to be returned by executing `Query`
   * @param {?Number} limit the maximum number of the requested entities; must go with `orderBy`
   * @param {?OrderBy} orderBy ordering of the resulting entities
   * @return {Query} a new query instance
   * @private
   */
  _newQuery(target, fieldMask, limit, orderBy) {
    const id = QueryFactory._newId();
    const actorContext = this._requestFactory._actorContext();

    const format = new ResponseFormat();
    format.setFieldMask(fieldMask);
    format.setLimit(limit);
    format.addOrderBy(orderBy);

    const result = new Query();
    result.setId(id);
    result.setTarget(target);
    result.setFormat(format);
    result.setContext(actorContext);

    return result;
  }

  /**
   * @return {QueryId}
   * @private
   */
  static _newId() {
    const result = new QueryId();
    result.setValue(`q-${newUuid()}`);
    return result;
  }
}

/**
 * A factory of `Command` instances.
 *
 * Uses the given `ActorRequestFactory` as the source of the command meta information,
 * such as the actor, the tenant, etc.
 *
 * @see ActorRequestFactory#command()
 */
class CommandFactory {

  constructor(actorRequestFactory) {
    this._requestFactory = actorRequestFactory;
  }

  /**
   * Creates a `Command` from the given command message.
   *
   * @param {!Message} message a command message
   * @return {Command} a Spine Command
   */
  create(message) {
    const id = CommandFactory._newCommandId();
    const messageAny = AnyPacker.packMessage(message);
    const context = this._commandContext();

    const result = new Command();
    result.setId(id);
    result.setMessage(messageAny);
    result.setContext(context);
    return result;
  }

  _commandContext() {
    const result = new CommandContext();
    const actorContext = this._requestFactory._actorContext();
    result.setActorContext(actorContext);
    return result;
  }

  /**
   * @return {CommandId}
   * @private
   */
  static _newCommandId() {
    const result = new CommandId();
    result.setUuid(newUuid());
    return result;
  }
}

/**
 * A builder for creating `Topic` instances. A more flexible approach to query creation
 * than using a `TopicFactory`.
 *
 * @extends {AbstractTargetBuilder<Topic>}
 * @template <T> a Protobuf type of the subscription target entities
 */
class TopicBuilder extends AbstractTargetBuilder {

  /**
   * @param {!Class<Message>} entity a Protobuf type of the subscription target entities
   * @param {!TopicFactory} topicFactory
   */
  constructor(entity, topicFactory) {
    super(entity);
    /**
     * @type {TopicFactory}
     * @private
     */
    this._factory = topicFactory;
  }

  /**
   * Creates the `Topic` instance based on the current builder configuration.
   *
   * @return {Topic} a new topic
   */
  build() {
    return this._factory.compose({
      forTarget: this.getTarget(),
      withMask: this.getMask(),
    });
  }
}

/**
 * A factory of {@link Topic} instances.
 *
 * Uses the given {@link ActorRequestFactory} as the source of the topic meta information,
 * such as the actor.
 *
 * @see ActorRequestFactory#topic()
 * @template <T> a Protobuf type of the subscription target entities
 */
class TopicFactory {

  /**
   * @param {!ActorRequestFactory} actorRequestFactory
   * @constructor
   */
  constructor(actorRequestFactory) {
    this._requestFactory = actorRequestFactory;
  }

  /**
   * Creates a new builder of `Topic` instances of the provided type.
   *
   * @param {!Class<Message>} entity a Protobuf type of the subscription target entities
   * @return {TopicBuilder}
   */
  select(entity) {
    return new TopicBuilder(entity, this);
  }

  /**
   * Creates a `Topic` for the specified `Target`.
   *
   * @param {!Target} forTarget a `Target` to create a topic for
   * @param {?FieldMask} withMask a mask specifying fields to be returned
   * @return {Topic} the instance of `Topic`
   */
  compose({forTarget: target, withMask: fieldMask}) {
    const id = TopicFactory._generateId();
    const topic = new Topic();
    topic.setId(id);
    topic.setContext(this._requestFactory._actorContext());
    topic.setTarget(target);
    topic.setFieldMask(fieldMask);
    return topic;
  }

  /**
   * @return {TopicId} a newly created topic ID
   * @private
   */
  static _generateId() {
    const topicId = new TopicId();
    topicId.setValue(`t-${newUuid()}`);
    return topicId;
  }
}

/**
 * A provider of the actor that is used to associate requests to the backend
 * with an application user.
 */
export class ActorProvider {

  /**
   * @param {?UserId} actor an optional actor to be used for identifying requests to the backend;
   *                        if not specified, the anonymous actor is used
   */
  constructor(actor) {
    this.update(actor);
  }

  /**
   * Updates the actor ID value if it is different from the current, sets the
   * anonymous actor value if actor ID not specified or `null`.
   *
   * @param {?UserId} actorId
   */
  update(actorId) {
    if (typeof actorId === 'undefined' || actorId === null) {
      this._actor = ActorProvider.ANONYMOUS;
    } else {
      ActorProvider._ensureUserId(actorId);

      if (!Message.equals(this._actor, actorId)) {
        this._actor = actorId;
      }
    }
  }

  /**
   * @return {UserId} the current actor value
   */
  get() {
    return this._actor;
  }

  /**
   * Sets the anonymous actor value.
   */
  clear() {
    this._actor = ActorProvider.ANONYMOUS;
  }

  /**
   * Ensures if the object extends {@link UserId}.
   *
   * The implementation doesn't use `instanceof` check and check on prototypes
   * since they may fail if different versions of the file are used at the same time
   * (e.g. bundled and the original one).
   *
   * @param object the object to check
   */
  static _ensureUserId(object) {
    if (!(isProtobufMessage(object) && typeof object.getValue === 'function')) {
      throw new Error('The `spine.core.UserId` type was expected by `ActorProvider`.');
    }
  }
}

/**
 * The anonymous backend actor.
 *
 * It is needed for requests to the backend when the particular user is undefined.
 *
 * @type UserId
 */
ActorProvider.ANONYMOUS = function () {
  const actor = new UserId();
  actor.setValue('ANONYMOUS');
  return actor;
}();

/**
 * A factory for the various requests fired from the client-side by an actor.
 */
export class ActorRequestFactory {

  /**
   * Creates a new instance of ActorRequestFactory for the given actor.
   *
   * @param {!ActorProvider} actorProvider a provider of an actor
   * @param {?TenantProvider} tenantProvider a provider of the current tenant, if omitted, the
   *                                         application is considered single-tenant
   */
  constructor(actorProvider, tenantProvider) {
    this._actorProvider = actorProvider;
    this._tenantProvider = tenantProvider;
  }

  /**
   * Creates a new `ActorRequestFactory` based on the passed options.
   *
   * @param {!ClientOptions} options the client initialization options
   * @return {ActorRequestFactory} a new `ActorRequestFactory` instance
   */
  static create(options) {
    if (!options) {
      throw new Error('Client options are not defined.')
    }
    const actorProvider = options.actorProvider;
    if (!actorProvider) {
      throw new Error('The actor provider should be set in the client options in order to ' +
          'construct an `ActorRequestFactory`.');
    }
    return new ActorRequestFactory(actorProvider, options.tenantProvider);
  }

  /**
   * Creates a new query factory for building various queries based on configuration of this
   * `ActorRequestFactory` instance.
   *
   * @return {QueryFactory}
   */
  query() {
    return new QueryFactory(this);
  }

  /**
   * Creates a new command factory for building various commands based on configuration of this
   * `ActorRequestFactory` instance.
   *
   * @return {CommandFactory}
   */
  command() {
    return new CommandFactory(this);
  }

  /**
   * Creates a new topic factory for building subscription topics based on configuration of this
   * `ActorRequestFactory` instance.
   *
   * @return {TopicFactory}
   */
  topic() {
    return new TopicFactory(this);
  }

  _actorContext() {
    const result = new ActorContext();
    if (this._tenantProvider) {
      result.setTenantId(this._tenantProvider.tenantId());
    }
    result.setActor(this._actorProvider.get());
    const seconds = Math.round(new Date().getTime() / 1000);
    const time = new Timestamp();
    time.setSeconds(seconds);
    result.setTimestamp(time);
    result.setZoneOffset(ActorRequestFactory._zoneOffset());
    return result;
  }

  /**
   * @return {ZoneOffset}
   * @protected
   */
  static _zoneOffset() {
    const format = new Intl.DateTimeFormat();
    const timeOptions = format.resolvedOptions();
    const zoneId = new ZoneId();
    zoneId.setValue(timeOptions.timeZone);
    const zoneOffset = ActorRequestFactory._zoneOffsetSeconds();
    const result = new ZoneOffset();
    result.setAmountSeconds(zoneOffset);
    return result;
  }

  /**
   * @return {number}
   * @private
   */
  static _zoneOffsetSeconds() {
    return new Date().getTimezoneOffset() * 60;
  }
}
