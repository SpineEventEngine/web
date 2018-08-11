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

import uuid from "uuid";

import timestamp from "spine-js-client-proto/google/protobuf/timestamp_pb";
import query from "spine-js-client-proto/spine/client/query_pb";
import entities from "spine-js-client-proto/spine/client/entities_pb";
import actorContext from "spine-js-client-proto/spine/core/actor_context_pb";
import command from "spine-js-client-proto/spine/core/command_pb";
import userId from "spine-js-client-proto/spine/core/user_id_pb";
import time from "spine-js-client-proto/spine/time/time_pb";
import {TypedMessage, TypeUrl} from "./typed-message";


/**
 * The type URL representing the spine.core.Command.
 *
 * @type {TypeUrl}
 */
const COMMAND_MESSAGE_TYPE = new TypeUrl("type.spine.io/spine.core.Command");

/**
 * A factory for the various requests fired from the client-side by an actor.
 */
export class ActorRequestFactory {

    /**
     * Creates a new instance of ActorRequestFactory for the given actor.
     *
     * @param actor a string identifier of an actor
     */
    constructor(actor) {
        this._actor = new userId.UserId();
        this._actor.setValue(actor);
    }

    /**
     * Creates a Query targeting all the instances of the given type.
     *
     * @param typeUrl the type URL of the target type; represented as a string
     * @return a {@link TypedMessage<Query>} of the built query
     */
    newQueryForAll(typeUrl) {
        let target = new entities.Target();
        target.setType(typeUrl.value);
        target.setIncludeAll(true);
        return this._newQuery(target);
    }

    /**
     * Creates a Query targeting a specific instance of the given type.
     *
     * @param typeUrl the type URL of the target type; represented as a string
     * @param id      the ID of the instance targeted by this query, represented
     *                as a {@link TypedMessage}
     * @return a {@link TypedMessage<Query>} of the built query
     */
    queryById(typeUrl, id) {
        const entityId = new entities.EntityId();
        entityId.setId(id.toAny());

        const idFilter = new entities.EntityIdFilter();
        idFilter.addIds(entityId);

        const filters = new entities.EntityFilters();
        filters.setIdFilter(idFilter);

        const target = new entities.Target();
        target.setType(typeUrl);
        target.setFilters(filters);

        return this._newQuery(target);
    }

    /**
     * Creates a Command from the given command message.
     *
     * @param message the command message, represented as a {@link TypedMessage}
     * @returns a {@link TypedMessage<Command>} of the built command
     */
    command(message) {
        let id = ActorRequestFactory._newCommandId();
        let messageAny = message.toAny();
        let context = this._commandContext();

        let result = new command.Command();
        result.setId(id);
        result.setMessage(messageAny);
        result.setContext(context);

        let typedResult = new TypedMessage(result, COMMAND_MESSAGE_TYPE);
        return typedResult;
    }

    _newQuery(target) {
        let id = ActorRequestFactory._newQueryId();
        let actorContext = this._actorContext();

        let result = new query.Query();
        result.setId(id);
        result.setTarget(target);
        result.setContext(actorContext);

        return result;
    }

    _actorContext() {
        let result = new actorContext.ActorContext();
        result.setActor(this._actor);
        let seconds = new Date().getUTCSeconds();
        let time = new timestamp.Timestamp();
        time.setSeconds(seconds);
        result.setTimestamp(time);
        result.setZoneOffset(ActorRequestFactory._zoneOffset());
        return result;
    }

    _commandContext() {
        let result = new command.CommandContext();
        let actorContext = this._actorContext();
        result.setActorContext(actorContext);
        return result;
    }

    static _zoneOffset() {
        let timeOptions = Intl.DateTimeFormat().resolvedOptions();
        let zoneId = new time.ZoneId();
        zoneId.setValue(timeOptions.timeZone);

        let zoneOffset = ActorRequestFactory._zoneOffsetSeconds();

        let result = new time.ZoneOffset();
        result.setAmountSeconds(zoneOffset);
        return result;
    }

    static _zoneOffsetSeconds() {
        return new Date().getTimezoneOffset() * 60;
    }

    static _newQueryId() {
        let result = new query.QueryId();
        result.setValue("q-" + uuid.v4());
        return result;
    }

    static _newCommandId() {
        let result = new command.CommandId();
        result.setUuid(uuid.v4());
        return result;
    }
}
