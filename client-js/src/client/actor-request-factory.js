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

"use strict";

import uuid from 'uuid';

import {Timestamp} from 'spine-web-client-proto/google/protobuf/timestamp_pb';
import {Query, QueryId} from 'spine-web-client-proto/spine/client/query_pb';
import {Topic, TopicId} from 'spine-web-client-proto/spine/client/subscription_pb';
import {
  ColumnFilter,
  CompositeColumnFilter,
  EntityFilters,
  EntityId,
  EntityIdFilter,
  Target
} from 'spine-web-client-proto/spine/client/entities_pb';
import {ActorContext} from 'spine-web-client-proto/spine/core/actor_context_pb';
import {Command, CommandContext, CommandId} from 'spine-web-client-proto/spine/core/command_pb';
import {UserId} from 'spine-web-client-proto/spine/core/user_id_pb';
import {ZoneId, ZoneOffset} from 'spine-web-client-proto/spine/time/time_pb';
import {FieldMask} from 'spine-web-client-proto/google/protobuf/field_mask_pb';
import {TypedMessage, TypeUrl} from './typed-message';


/**
 * The type URL representing the spine.core.Command.
 *
 * @type {TypeUrl}
 */
const COMMAND_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.core.Command');

class ColumnFilters {
  /**
   * @param {ColumnFilter[]} filters
   * @return {CompositeColumnFilter}
   */
  static all(filters) {
    return ColumnFilters.compose(filters, CompositeColumnFilter.CompositeOperator.ALL);
  }

  /**
   * @param {ColumnFilter[]} filters
   * @return {CompositeColumnFilter}
   */
  static either(filters) {
    return ColumnFilters.compose(filters, CompositeColumnFilter.CompositeOperator.EITHER);
  }

  /**
   *
   * @param {ColumnFilter[]} filters
   * @param {CompositeColumnFilter.CompositeOperator} operator
   * @return {CompositeColumnFilter}
   */
  static compose(filters, operator) {
    const compositeFilter = new CompositeColumnFilter();
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
    throw 'Tried instantiating a utility class.';
  }

  /**
   * Composes a new target for entities of specified type, optionally with specified IDs and
   * columnFilters.
   *
   * @param {!TypeUrl} type a Type URL for all target entities to match
   * @param {?TypedMessage[]} ids an array of IDs one of which must be matched by each target entity
   * @param {?CompositeColumnFilter[]} columnFilters an array of filters target
   *
   * @return {Target} a newly created target for entities with specified filters
   */
  static compose(type, ids, columnFilters) {
    const includeAll = !ids && !columnFilters;

    if (includeAll) {
      return Targets._all(type);
    }

    const filters = new EntityFilters();

    const entityIds = Targets._nullToEmpty(ids);
    const idFilter = Targets._assembleIdFilter(entityIds);
    filters.setIdFilter(idFilter);

    const entityColumnValues = Targets._nullToEmpty(columnFilters);
    filters.setFilterList(entityColumnValues);

    return Targets._filtered(type, filters);
  }

  /**
   * Creates a new target including all items of type.
   *
   * @param {!TypeUrl} type
   * @return {Target}
   * @private
   */
  static _all(type) {
    const target = new Target();
    target.setType(type.value);
    target.setIncludeAll(true);
    return target;
  }


  /**
   * Creates a new target including only entities of the specified type that pass filtering.
   *
   * @param {!TypeUrl} type
   * @param {!EntityFilters} filters
   * @return {Target}
   * @private
   */
  static _filtered(type, filters) {
    const target = new Target();
    target.setType(type.value);
    target.setFilters(filters);
    return target;
  }

  /**
   * Creates an targets ID filter including only items which are included in the provided ID list.
   *
   * @param {!TypedMessage} entityIds an array of IDs for entities matching target to be included in
   * @return {EntityIdFilter}
   * @private
   */
  static _assembleIdFilter(entityIds) {
    const idFilter = new EntityIdFilter();
    entityIds.forEach(rawId => {
      const packedId = rawId.toAny();
      const entityId = new EntityId();
      entityId.setId(packedId);
      idFilter.addIds(entityId);
    });
    return idFilter;
  }

  /**
   * @param {!Array<T>} input
   * @return {Array<T>} an empty array if the value is `null`, or the provided input otherwise
   * @template <T>
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
  'either ColumnFilter or CompositeColumnFilter.';

/**
 * A builder for creating `Query` instances. A more flexible approach to query creation
 * than using a `QueryFactory`.
 */
class QueryBuilder {

  constructor(typeUrl, queryFactory) {
    this._typeUrl = typeUrl;
    /**
     * @type {QueryFactory}
     * @private
     */
    this._factory = queryFactory;
    /**
     * @type {TypedMessage[]}
     * @private
     */
    this._ids = null;
    /**
     * @type {CompositeColumnFilter[]}
     * @private
     */
    this._columns = null;
  }

  /**
   * Sets an ID predicate of the `Query#getTarget()`.
   *
   * Makes the query return only the entities identified by the provided IDs.
   *
   * @param {!TypedMessage[]} ids an array with identifiers of entities to query
   * @return {QueryBuilder} the current builder instance
   * @throws if this method is executed more than once
   */
  byIds(ids) {
    if (this._ids !== null) {
      throw 'Can not set query ID more than once for QueryBuilder.';
    }
    this._ids = ids.slice();
    return this;
  }

  /**
   * Sets an Entity Column predicate of the `Query#getTarget()`.
   *
   * <p>If there are no `ColumnFilter`s (i.e. the provided array is empty), all
   * the records will be returned by executing the `Query`.
   *
   * <p>An array of predicates provided to this method are considered to be joined in
   * a conjunction (using `CompositeColumnFilter.CompositeOperator#ALL`). This means
   * a record would match this query only if it matches all of the predicates.
   *
   * @param {!ColumnFilter[]|CompositeColumnFilter[]} predicates
   * the predicates to filter the requested entities by
   * @return {QueryBuilder} self for method chaining
   * @throws if this method is executed more than once
   * @see ColumnFilters a convenient way to create `ColumnFilter`s
   */
  where(predicates) {
    if (this._columns !== null) {
      throw 'Can not set filters more than once for QueryBuilder.';
    }
    if (predicates[0] instanceof ColumnFilter) {
      QueryBuilder._checkAllOfType(predicates, ColumnFilter, INVALID_FILTER_TYPE);
      const aggregatingFilter = ColumnFilters.all(predicates);
      this._columns = [aggregatingFilter];
    } else {
      QueryBuilder._checkAllOfType(predicates, CompositeColumnFilter, INVALID_FILTER_TYPE);
      this._columns = predicates.slice();
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
   * @return {QueryBuilder} self for method chaining
   * @throws if this method is executed more than once
   * @see FieldMask
   */
  withMask(fieldNames) {
    if (typeof this._fieldMask !== 'undefined') {
      throw 'Can not set field mask more than once for QueryBuilder.';
    }
    if (!fieldNames.length) {
      return this;
    }
    this._fieldMask = new FieldMask();
    this._fieldMask.setPathsList(fieldNames);
    return this;
  }

  /**
   * Creates the Query instance based on the parameters set to this builder.
   *
   * @return {Query} a new query
   */
  build() {
    const target = Targets.compose(this._typeUrl, this._ids, this._columns);
    return this._factory.forTarget(target, this._fieldMask);
  }

  /**
   * Checks that each provided item is an instance of the provided class. In case the check does
   * not pass an error is thrown.
   *
   * @param {![]} items an array of objects that are expected to be of the provided type
   * @param {!Object} cls a class each item is required to be instance of
   * @param {!String} message
   * @private
   */
  static _checkAllOfType(items, cls, message = 'Unexpected parameter type.') {
    items.forEach(item => {
      if (!(item instanceof cls)) {
        throw message;
      }
    });
  }
}

/**
 * A factory for creating `Query` instances specifying the data to be retrieved from Spine server.
 *
 * @see ActorRequestFactory#query()
 */
class QueryFactory {

  /**
   * @param {!ActorRequestFactory} requestFactory
   */
  constructor(requestFactory) {
    this._requestFactory = requestFactory;
  }

  /**
   * Creates a new builder of Query instances of the provided type
   * @param {!TypeUrl} type a type URL of the target type
   * @return {QueryBuilder}
   */
  select(type) {
    return new QueryBuilder(type, this);
  }

  /**
   * Creates a new `Query` which would return only entities which conform the target specification.
   *
   * An optional field mask can be provided to specify particular fields to be returned for `Query`
   *
   * @param {!Target} target a specification of type and filters for `Query` result to match
   * @param {?FieldMask} fieldMask a specification of fields to be returned by executing `Query`
   * @return {Query}
   */
  forTarget(target, fieldMask) {
    return this._newQuery(target, fieldMask)
  }

  /**
   * @param {!Target} target a specification of type and filters for `Query` result to match
   * @param {?FieldMask} fieldMask a specification of fields to be returned by executing `Query`
   * @return {Query} a new query instance
   * @private
   */
  _newQuery(target, fieldMask) {
    const id = QueryFactory._newId();
    const actorContext = this._requestFactory._actorContext();

    const result = new Query();
    result.setId(id);
    result.setTarget(target);
    result.setFieldMask(fieldMask);
    result.setContext(actorContext);

    return result;
  }

  /**
   * @return {QueryId}
   * @private
   */
  static _newId() {
    const result = new QueryId();
    result.setValue(`q-${uuid.v4()}`);
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
   * @param {!TypedMessage} message a typed command message
   * @return {TypedMessage<Command>} a typed representation of the Spine Command
   */
  create(message) {
    const id = CommandFactory._newCommandId();
    const messageAny = message.toAny();
    const context = this._commandContext();

    const result = new Command();
    result.setId(id);
    result.setMessage(messageAny);
    result.setContext(context);
    return new TypedMessage(result, COMMAND_MESSAGE_TYPE);
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
    result.setUuid(uuid.v4());
    return result;
  }
}

/**
 * A factory of {@link Topic} instances.
 *
 * Uses the given {@link ActorRequestFactory} as the source of the topic meta information,
 * such as the actor.
 *
 * @see ActorRequestFactory#topic()
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
   * Creates a {@link Topic} for the entity states with the given IDs.
   *
   * @param {!TypeUrl} typeUrl the class of a target entity
   * @param {!TypedMessage[]} ids the IDs of interest
   * @return {Topic} the instance of {@code Topic} assembled according to the parameters
   */
  someOf(typeUrl, ids) {
    const target = Targets.compose(typeUrl, ids);
    return this._forTarget(target);
  }

  /**
   * Creates a {@link Topic} for all of the specified entity states.
   *
   * @param {!TypeUrl} typeUrl the class of a target entity
   * @return {Topic} an instance of {@code Topic} assembled according to the parameters
   */
  allOf(typeUrl) {
    const target = Targets.compose(typeUrl);
    return this._forTarget(target);
  }

  /**
   * Creates a {@link Topic} for the specified {@link Target}.
   *
   * @param {!Target} target the {@code Target} to create a topic for
   * @return {Topic} the instance of {@code Topic}
   * @private
   */
  _forTarget(target) {
    const id = TopicFactory._generateId();
    const topic = new Topic();
    topic.setId(id);
    topic.setContext(this._requestFactory._actorContext());
    topic.setTarget(target);
    return topic;
  }

  static _generateId() {
    const topicId = new TopicId();
    topicId.setValue(`t-${uuid.v4()}`);
    return topicId;
  }
}

/**
 * A factory for the various requests fired from the client-side by an actor.
 */
export class ActorRequestFactory {

  /**
   * Creates a new instance of ActorRequestFactory for the given actor.
   *
   * @param {!string} actor a string identifier of an actor
   */
  constructor(actor) {
    this._actor = new UserId();
    this._actor.setValue(actor);
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
    result.setActor(this._actor);
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
