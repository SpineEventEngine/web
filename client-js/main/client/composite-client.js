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

"use strict";

import {Message} from 'google-protobuf';
import ObjectToProto from "./object-to-proto";
import {CommandHandlingError, CommandValidationError, SpineError} from "./errors";
import {Status} from '../proto/spine/core/response_pb';
import {Client} from "./client";
import {CommandRequest, QueryRequest, SubscriptionRequest} from "./client-request";

/**
 * A {@link Client} that delegates requests to case-specific client implementations.
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

    /**
     * @override
     */
    command(command) {
        this._commanding.command(command);
    }

    /**
     * @override
     */
    select(entityType) {
        this._querying.select(entityType);
    }

    /**
     * @override
     */
    subscribeTo(type) {
        this._subscribing.subscribeTo(type);
    }
}

/**
 * A client which performs entity state queries.
 *
 * @abstract
 */
export class QueryingClient {

    /**
     * @param {!ActorRequestFactory} actorRequestFactory
     *        a request factory to build requests to Spine server
     */
    constructor(actorRequestFactory) {
        this.requestFactory = actorRequestFactory;
    }

    /**
     * @param {!Class<? extend Message>} entityType a Protobuf type of the query target entities
     * @return {QueryRequest}
     */
    select(entityType) {
        return new QueryRequest(entityType, this);
    }

    /**
     * Executes the given `Query` instance specifying the data to be retrieved from
     * Spine server fulfilling a returned promise with an array of received objects.
     *
     * @param {!Query} query a query instance to be executed
     * @return {Promise<<T extends Message>[]>} a promise to be fulfilled with a list of Protobuf
     *        messages of a given type or with an empty list if no entities matching given query
     *        were found; rejected with a `SpineError` if error occurs
     *
     * @template <T> a Protobuf type of entities being the target of a query
     */
    execute(query) {
        throw new Error('Not implemented in abstract base.');
    }
}

/**
 * A client which manages entity state and event subscriptions.
 *
 * @abstract
 */
export class SubscribingClient {

    /**
     * @param {!ActorRequestFactory} actorRequestFactory
     *        a request factory to build requests to Spine server
     */
    constructor(actorRequestFactory) {
        this.requestFactory = actorRequestFactory;
    }

    subscribeTo(type) {
        return new SubscriptionRequest(type, this);
    }

    /**
     * @return {Promise<EntitySubscriptionObject<T extends Message>>}
     *
     * @template <T> a Protobuf type of entities being the target of a subscription
     */
    subscribe(topic) {
        throw new Error('Not implemented in abstract base.');
    }
}

const SUBSCRIPTIONS_NOT_SUPPORTED = 'Subscriptions are not supported.';

/**
 * A {@link SubscribingClient} which does not create subscriptions.
 */
export class NoOpSubscribingClient extends SubscribingClient {

    constructor(actorRequestFactory) {
        super(actorRequestFactory)
    }

    /**
     * Always throws an error.
     *
     * @override
     */
    subscribeTo(type) {
        throw new Error(SUBSCRIPTIONS_NOT_SUPPORTED);
    }

    /**
     * Always throws an error.
     *
     * @override
     */
    subscribe(topic) {
        throw new Error(SUBSCRIPTIONS_NOT_SUPPORTED);
    }
}

const _statusType = Status.typeUrl();

/**
 * A client which posts commands.
 *
 * This class has a default implementation. Override it to change the behaviour.
 */
export class CommandingClient {

    constructor(endpoint, requestFactory) {
        this.requestFactory = requestFactory;
        this._endpoint = endpoint;
    }

    command(commandMessage) {
        return new CommandRequest(commandMessage, this);
    }

    sendCommand(commandMessage, acknowledgedCallback, errorCallback, rejectionCallback) {
        const command = this.requestFactory.command().create(commandMessage);
        this._endpoint.command(command)
            .then(ack => this._onAck(ack, acknowledgedCallback, errorCallback, rejectionCallback))
            .catch(error => {
                errorCallback(new CommandHandlingError(error.message, error));
            });
    }

    _onAck(ack, acknowledgedCallback, errorCallback, rejectionCallback) {
        const responseStatus = ack.status;
        const responseStatusProto = ObjectToProto.convert(responseStatus, _statusType);
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
 *  - the all entities if no IDs specified.
 *
 * @param {AbstractTargetBuilder<Query|Topic>} targetBuilder
 *      a builder for creating `Query` or `Topic` instances.
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
