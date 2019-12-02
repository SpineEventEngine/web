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
import {CompositeFilter, Filter} from '../proto/spine/client/filters_pb';
import {OrderBy} from '../proto/spine/client/query_pb';
import {MessageId, Origin} from '../proto/spine/core/diagnostics_pb';
import {AnyPacker} from "./any-packer";
import {Filters} from "./actor-request-factory";

/**
 * @abstract
 */
class ClientRequest {

    /**
     * @param {!Client} client
     * @param {!ActorRequestFactory} actorRequestFactory
     */
    constructor(client, actorRequestFactory) {
        this.client = client;
        this.actorRequestFactory = actorRequestFactory;
    }
}

/**
 * @abstract
 */
class FilteringRequest extends ClientRequest {

    /**
     * @param {!Class<? extends Message>} targetType
     * @param {!Client} client
     * @param {!ActorRequestFactory} actorRequestFactory
     */
    constructor(targetType, client, actorRequestFactory) {
        super(client, actorRequestFactory);
        this.targetType = targetType;
    }

    /**
     * @param ids {!<I extends Message>[]|Number[]|String[]}
     * @return {FilteringRequest} self
     *
     * @template <I> a Protobuf type of IDs
     */
    byId(ids) {
        this.builder().byIds(ids);
        return this.self();
    }

    /**
     * @param {!Filter[]|CompositeFilter[]} predicates
     * @return {FilteringRequest} self
     */
    where(predicates) {
        this.builder().where(predicates);
        return this.self();
    }

    /**
     * @param {!String[]} fieldNames
     * @return {FilteringRequest} self
     */
    withMask(fieldNames) {
        this.builder().withMask(fieldNames);
        return this.self();
    }

    builder() {
        // TODO:2019-11-27:dmytro.kuzmin:WIP Check that setting to some initial value is
        //  unnecessary.
        if (!this._builder) {
            this._builder = this.newBuilderFn().apply(this.actorRequestFactory);
        }
        return this._builder;
    }

    /**
     * @abstract
     *
     * @return {Function<ActorRequestFactory, AbstractTargetBuilder>}
     */
    newBuilderFn() {
        throw new Error('Not implemented in abstract base.');
    }

    /**
     * @abstract
     *
     * @return {FilteringRequest} self
     */
    self() {
        throw new Error('Not implemented in abstract base.');
    }
}

export class QueryRequest extends FilteringRequest {

    constructor(targetType, client, actorRequestFactory) {
        super(targetType, client, actorRequestFactory)
    }

    // TODO:2019-11-27:dmytro.kuzmin:WIP See what we can do about it.
    // noinspection JSValidateJSDoc unresolved nested type which actually exists
    /**
     *
     * @param {!String} column
     * @param {!OrderBy.Direction} direction
     * @return {QueryRequest} self
     */
    orderBy(column, direction) {
        if (direction === OrderBy.Direction.ASCENDING) {
            this.builder().orderAscendingBy(column);
        } else {
            this.builder().orderDescendingBy(column);
        }
        return this.self();
    }

    /**
     * @param {number} count the max number of response entities
     * @return {QueryRequest} self
     */
    limit(count) {
        this.builder().limit(count);
        return this.self();
    }

    /**
     * @return {Promise<<T extends Message>[]>}
     *
     * @template <T> a Protobuf type of entities being the target of a query
     */
    run() {
        const query = this.builder().build();
        return this.client.read(query);
    }

    newBuilderFn() {
        return requestFactory => requestFactory.query().select(this.targetType);
    }

    self() {
        return this;
    }
}

/**
 * @abstract
 */
class SubscribingRequest extends FilteringRequest {

    /**
     * @return {Promise<EntitySubscriptionObject<T extends Message>>}
     *
     * @template <T> a Protobuf type of entities being the target of a subscription
     */
    post() {
        const topic = this.builder().build();
        return this.doSubscribe(topic);
    }

    newBuilderFn() {
        return requestFactory => requestFactory.topic().select(this.targetType);
    }

    self() {
        return this;
    }

    doSubscribe(topic) {
        throw new Error('Not implemented in abstract base.');
    }
}

export class SubscriptionRequest extends SubscribingRequest {

    constructor(entityType, client, actorRequestFactory) {
        super(entityType, client, actorRequestFactory)
    }

    /**
     * @override
     */
    doSubscribe(topic) {
        return this.client.subscribe(topic);
    }
}

export class EventSubscriptionRequest extends SubscribingRequest {

    constructor(eventType, client, actorRequestFactory) {
        super(eventType, client, actorRequestFactory)
    }

    /**
     * @override
     */
    doSubscribe(topic) {
        return this.client.subscribeToEvents(topic);
    }
}

const NOOP_CALLBACK = () => {};

export class CommandRequest extends ClientRequest{

    /**
     * @param {!Message} commandMessage
     * @param {!Client} client
     * @param {!ActorRequestFactory} actorRequestFactory
     */
    constructor(commandMessage, client, actorRequestFactory) {
        super(client, actorRequestFactory);
        this._commandMessage = commandMessage;
        this._onAck = NOOP_CALLBACK;
        this._onError = NOOP_CALLBACK;
        this._onRejection = NOOP_CALLBACK;
        this._observedTypes = [];
    }

    /**
     * @param {!parameterlessCallback} callback
     *
     * @return {CommandRequest} self
     */
    onAck(callback) {
        this._onAck = callback;
        return this;
    }

    /**
     * @param {!consumerCallback} callback
     *
     * @return {CommandRequest} self
     */
    onError(callback) {
        this._onError = callback;
        return this;
    }

    /**
     * @param {!consumerCallback} callback
     *
     * @return {CommandRequest} self
     */
    onRejection(callback) {
        this._onRejection = callback;
        return this;
    }

    /**
     * @param {!Class<? extends Message>} eventType a Protobuf type of the observed events
     *
     * @return {CommandRequest} self
     */
    observe(eventType) {
        this._observedTypes.push(eventType);
        return this;
    }

    /**
     * @return {Promise<EntitySubscriptionObject[] | EntitySubscriptionObject>}
     */
    post() {
        const command = this.actorRequestFactory.command().create(this._commandMessage);
        const ackCallback =
            {onOk: this._onAck, onError: this._onError, onRejection: this._onRejection};
        this.client.post(command, ackCallback);
        const promises = [];
        this._observedTypes.forEach(type => {
            const originFilter = Filters.eq("context.past_message", this._asOrigin(command));
            const promise = this.client.subscribeTo(type)
                .where([originFilter])
                .post();
            promises.push(promise);
        });
        if (promises.length === 1) {
            return promises[0];
        }
        return Promise.all(promises);
    }

    /**
     * @param {!Command} command
     *
     * @return {Origin}
     *
     * @private
     */
    _asOrigin(command) {
        const result = new Origin();

        const messageId = new MessageId();
        const packedId = AnyPacker.pack(command.getId());
        messageId.setId(packedId);
        const typeUrl = command.getMessage().getTypeUrl();
        messageId.setTypeUrl(typeUrl);
        result.setMessage(messageId);

        const grandOrigin = command.getContext().getOrigin();
        result.setGrandOrigin(grandOrigin);

        const actorContext = command.getContext().getActorContext();
        result.setActorContext(actorContext);
        return result;
    }
}
