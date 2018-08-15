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

import {Timestamp} from 'spine-js-client-proto/google/protobuf/timestamp_pb';
import {Query, QueryId} from 'spine-js-client-proto/spine/client/query_pb';
import {
  EntityFilters,
  EntityId,
  EntityIdFilter,
  Target
} from 'spine-js-client-proto/spine/client/entities_pb';
import {ActorContext} from 'spine-js-client-proto/spine/core/actor_context_pb';
import {Command, CommandContext, CommandId} from 'spine-js-client-proto/spine/core/command_pb';
import {UserId} from 'spine-js-client-proto/spine/core/user_id_pb';
import {ZoneId, ZoneOffset} from 'spine-js-client-proto/spine/time/time_pb';
import {TypedMessage, TypeUrl} from './typed-message';


/**
 * The type URL representing the spine.core.Command.
 *
 * @type {TypeUrl}
 */
const COMMAND_MESSAGE_TYPE = new TypeUrl('type.spine.io/spine.core.Command');

class QueryBuilder {
  constructor(typeUrl, queryFactory) {
    this._typeUrl = typeUrl;
    this._factory = queryFactory;
    this._ids = null;
  }

  /**
   * @param id
   * @return {QueryBuilder}
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
   * @return {Query}
   */
  build() {
    return this._factory.byIds(this._typeUrl, this._ids);
  }
}

class QueryFactory {

  /**
   * @param {ActorRequestFactory} requestFactory
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
   * @return {Query}
   */
  byIds(typeUrl, ids) {
    const target = new Target();
    target.setType(typeUrl.value);
    
    if (ids !== null) {
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
    result.setValue('q-' + uuid.v4());
    return result;
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

  query() {
    return new QueryFactory(this);
  }

  /**
   * Creates a Command from the given command message.
   *
   * @param {!TypedMessage} message a typed command message
   * @return {TypedMessage<Command>} a typed representation of the Spine Command
   */
  command(message) {
    const id = ActorRequestFactory._newCommandId();
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

    const actorContext = this._actorContext();
    result.setActorContext(actorContext);

    return result;
  }

  _actorContext() {
    const result = new ActorContext();
    result.setActor(this._actor);
    const seconds = new Date().getUTCSeconds();
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
