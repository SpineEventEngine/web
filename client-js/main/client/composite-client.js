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

import ObjectToProto from "./object-to-proto";
import {CommandHandlingError, CommandValidationError, SpineError} from "./errors";
import {Status} from '../proto/spine/core/response_pb';
import {Client} from "./client";

/**
 * A {@link Client} delegates requests to case-specific client implementations.
 *
 * @see QueryingClient
 * @see SubscribingClient
 * @see CommandingClient
 */
export class CompositeClient extends Client {

    constructor(querying, subscribing, commanding) {
        super();

        /**
         * @type QueryingClient
         *
         * @private
         */
        this._querying = querying;

        /**
         * @type SubscribingClient
         *
         * @private
         */
        this._subscribing = subscribing;

        /**
         * @type CommandingClient
         *
         * @private
         */
        this._commanding = commanding;
    }

    newQuery() {
        return this._querying.newQuery();
    }

    execute(query) {
        return this._querying.execute(query);
    }

    fetch({entity: cls, byIds: ids}) {
        return this._querying.fetch(cls, ids);
    }

    newTopic() {
        return this._subscribing.newTopic();
    }

    subscribeTo(topic) {
        return this._subscribing.subscribeTo(topic);
    }

    subscribe({entity: cls, byIds: ids}) {
        return this._subscribing.subscribe(cls, ids);
    }

    sendCommand(commandMessage, acknowledgedCallback, errorCallback, rejectionCallback) {
        return this._commanding.sendCommand(commandMessage,
                                            acknowledgedCallback,
                                            errorCallback,
                                            rejectionCallback);
    }
}

/**
 * A client which performs entity state queries.
 */
export class QueryingClient {

    /**
     * @param {!ActorRequestFactory} actorRequestFactory
     *        a request factory to build requests to Spine server
     */
    constructor(actorRequestFactory) {
        this._requestFactory = actorRequestFactory;
    }

    /**
     * Executes the given `Query` instance specifying the data to be retrieved from
     * Spine server fulfilling a returned promise with an array of received objects.
     *
     * @param {!Query} query a query instance to be executed
     * @return {Promise<<T extends Message>[]>} a promise to be fulfilled with a list of Protobuf
     *        messages of a given type or with an empty list if no entities matching given query
     *        were found; rejected with a `SpineError` if error occurs;
     *
     * @template <T> a Protobuf type of entities being the target of a query
     */
    execute(query) {
        throw new Error('Not implemented in abstract base.');
    }

    newQuery() {
        return this._requestFactory.query();
    }

    fetch(entityClass, ids) {
        const queryBuilder = this.newQuery().select(entityClass);
        const query = _buildTarget(queryBuilder, ids);
        return this.execute(query);
    }
}

/**
 * A client which manages entity state and event subscriptions.
 */
export class SubscribingClient {

    /**
     * @param {!ActorRequestFactory} actorRequestFactory
     *        a request factory to build requests to Spine server
     */
    constructor(actorRequestFactory) {
        this._requestFactory = actorRequestFactory;
    }

    subscribeTo(topic) {
        throw new Error('Not implemented in abstract base.');
    }

    newTopic() {
        return this._requestFactory.topic();
    }

    subscribe(entityClass, ids) {
        const topicBuilder = this.newTopic().select(entityClass);
        const topic = _buildTarget(topicBuilder, ids);

        return this.subscribeTo(topic);
    }
}

/**
 * A client which posts commands.
 */
export class CommandingClient {

    constructor(endpoint, requestFactory) {
        this._requestFactory = requestFactory;
        this._endpoint = endpoint;
    }

    sendCommand(commandMessage, acknowledgedCallback, errorCallback, rejectionCallback) {
        const command = this._requestFactory.command().create(commandMessage);
        this._endpoint.command(command)
            .then(ack => this._onAck(ack, acknowledgedCallback, errorCallback, rejectionCallback))
            .catch(error => {
                errorCallback(new CommandHandlingError(error.message, error));
            });
    }

    _onAck(ack, acknowledgedCallback, errorCallback, rejectionCallback) {
        const responseStatus = ack.status;
        const responseStatusProto = ObjectToProto.convert(responseStatus, Status.typeUrl());
        const responseStatusCase = responseStatusProto.getStatusCase();

        switch (responseStatusCase) {
            case Status.StatusCase.OK:
                acknowledgedCallback();
                break;
            case Status.StatusCase.ERROR:
                const error = responseStatusProto.getError();
                const message = error.getMessage();
                errorCallback(error.hasValidationError()
                    ? new CommandValidationError(message, error)
                    : new CommandHandlingError(message, error));
                break;
            case Status.StatusCase.REJECTION:
                rejectionCallback(responseStatusProto.getRejection());
                break;
            default:
                errorCallback(
                    new SpineError(`Unknown response status case ${responseStatusCase}`)
                );
        }
    }
}

/**
 * Builds target from the given target builder specifying the set
 * of target entities.
 *
 * The resulting target points to:
 *  - the particular entities with the given IDs;
 *  - the all entities if no IDs specified;
 *
 * @param {AbstractTargetBuilder<Query|Topic>} targetBuilder
 *    a builder for creating `Query` or `Topic` instances.
 * @param {?<T extends Message>[] | <T extends Message> | Number[] | Number | String[] | String} ids
 *      a list of target entities IDs or an ID of a single target entity
 * @return {Query|Topic} the built target
 *
 * @template <T> a class of identifiers, corresponding to the query/subscription targets
 * @private
 */
function _buildTarget(targetBuilder, ids) {
    if (Array.isArray(ids)) {
        targetBuilder.byIds(ids);
    } else if (!!ids) {
        targetBuilder.byIds([ids]);
    }
    return targetBuilder.build();
}
