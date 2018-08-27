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
  EntityFilters,
  EntityId,
  EntityIdFilter,
  Target
} from 'spine-web-client-proto/spine/client/entities_pb';
import {ActorContext} from 'spine-web-client-proto/spine/core/actor_context_pb';
import {Command, CommandContext, CommandId} from 'spine-web-client-proto/spine/core/command_pb';
import {UserId} from 'spine-web-client-proto/spine/core/user_id_pb';
import {ZoneId, ZoneOffset} from 'spine-web-client-proto/spine/time/time_pb';
import {TypedMessage, TypeUrl} from './typed-message';


/**
 * The type URL representing the spine.core.Command.
 *
 * @type {TypeUrl}
 */
const COMMAND_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.core.Command');

/**
 * A builder for creating `Query` instances. A more flexible approach to query creation
 * than using a `QueryFactory`.
 */
class QueryBuilder {

  constructor(typeUrl, queryFactory) {
    this._typeUrl = typeUrl;
    this._factory = queryFactory;
    this._ids = null;
  }

  /**
   * Makes the query to return only the object defined by the provided identifiers.
   *
   * @param {!Message|Message[]} id an entity ID or an array of entity IDs to query
   * @return {QueryBuilder} the current builder instance
   * @throws if this method is executed more than once
   */
  byId(id) {
    if (this._ids !== null) {
      throw "Can not set query id more than once.";
    }
    if (id instanceof Array) {
      this._ids = id.map(each => each);
    } else {
      this._ids = [id];
    }

    return this;
  }

  /**
   * Creates the Query instance based on all set parameters.
   *
   * @return {Query} a new query
   */
  build() {
    return this._factory.byIds(this._typeUrl, this._ids);
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
   * @param {!TypeUrl} typeUrl a type URL of the target type
   * @return {QueryBuilder}
   */
  select(typeUrl) {
    return new QueryBuilder(typeUrl, this);
  }

  /**
   * Creates a new query which returns entities of a matching type with specified IDs.
   *
   * If no identifiers are provided queries for all objects of the provided type.
   *
   * @param {!TypeUrl} typeUrl
   * @param {!TypedMessage[]} ids a list of entity identifier messages; an empty list is acceptable
   * @return {Query} a new query
   */
  byIds(typeUrl, ids) {
    const target = new Target();
    target.setType(typeUrl.value);

    if (ids !== null && ids.length) {
      const idFilter = new EntityIdFilter();
      ids.forEach(id => {
        const entityId = new EntityId();
        entityId.setId(id.toAny());

        idFilter.addIds(entityId);
      });
      const filters = new EntityFilters();
      filters.setIdFilter(idFilter);
      target.setFilters(filters);
    } else {
      target.setIncludeAll(true);
    }

    return this._newQuery(target);
  }

  /**
   * @param {!Target} target a target of the query
   * @return {Query} a new query instance
   * @private
   */
  _newQuery(target) {
    const id = QueryFactory._newQueryId();
    const actorContext = this._requestFactory._actorContext();

    const result = new Query();
    result.setId(id);
    result.setTarget(target);
    result.setContext(actorContext);

    return result;
  }

  /**
   * @return {QueryId}
   * @private
   */
  static _newQueryId() {
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
   * Creates a Command from the given command message.
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
    const target = TopicFactory._composeTarget(typeUrl, ids);
    return this._forTarget(target);
  }

  /**
   * Creates a {@link Topic} for all of the specified entity states.
   *
   * @param {!TypeUrl} typeUrl the class of a target entity
   * @return {Topic} an instance of {@code Topic} assembled according to the parameters
   */
  allOf(typeUrl) {
    const target = TopicFactory._composeTarget(typeUrl);
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

  static _composeTarget(typeUrl, ids, columnFilters) {
    const includeAll = (!ids || !ids.length) && (!columnFilters || !columnFilters.length);

    const filters = new EntityFilters();
    if (!includeAll) {
      const entityIds = TopicFactory._nullToEmpty(ids);
      const idFilters = this._assembleIdFilters(entityIds);
      filters.setIdFilter(idFilters);
    }
    const entityColumnValues = TopicFactory._nullToEmpty(columnFilters);
    filters.setFilterList(entityColumnValues);

    return this._newTarget(typeUrl, includeAll, filters);
  }

  static _newTarget(typeUrl, includeAll, filters) {
    const target = new Target();
    target.setType(typeUrl.value);
    if (includeAll) {
      target.setIncludeAll(true);
    } else {
      target.setFilters(filters);
    }
    return target;
  }

  static _assembleIdFilters(entityIds) {
    const idFilter = new EntityIdFilter();
    entityIds.forEach(rawId => {
      const packedId = rawId.toAny();
      const entityId = new EntityId();
      entityId.setId(packedId);
      idFilter.addIds(entityId);
    });
    return idFilter;
  }

  static _nullToEmpty(input) {
    if (input == null) {
      return [];
    } else {
      return input;
    }
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
