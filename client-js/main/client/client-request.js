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

class FilteringRequest {

    /**
     * @param {!Class<? extends Message>} targetType
     * @param {!QueryingClient | SubscribingClient} client
     */
    constructor(targetType, client) {
        this._targetType = targetType;
        this._client = client;
        this._requestFactory = client.requestFactory;
        this._builder = null;
    }

    /**
     *
     * @param ids {!<I extends Message>[]|Number[]|String[]}
     * @return {QueryRequest | SubscriptionRequest} self
     *
     * @template <I> a Protobuf type of IDs
     */
    byId(ids) {
        this.builder().byIds(ids);
        return this.self();
    }

    /**
     * @param {!Filter[]|CompositeFilter[]} predicates
     * @return {QueryRequest | SubscriptionRequest} self
     */
    where(predicates) {
        this.builder().where(predicates);
        return this.self();
    }

    /**
     * @param {!String[]} fieldNames
     * @return {QueryRequest | SubscriptionRequest} self
     */
    withMask(fieldNames) {
        this.builder().withMask(fieldNames);
        return this.self();
    }

    targetType() {
        return this._targetType;
    }

    client() {
        return this._client;
    }

    builder() {
        if (!this._builder) {
            this._builder = this.builderFn().apply(this._requestFactory);
        }
        return this._builder;
    }

    /**
     * @abstract
     *
     * @return {Function<ActorRequestFactory, AbstractTargetBuilder>}
     */
    builderFn() {
        throw new Error('Not implemented in abstract base.');
    }

    /**
     * @abstract
     *
     * @return {QueryRequest | SubscriptionRequest}
     */
    self() {
        throw new Error('Not implemented in abstract base.');
    }
}

export class QueryRequest extends FilteringRequest {

    constructor(targetType, client) {
        super(targetType, client)
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
        return this.client().execute(query);
    }

    builderFn() {
        return requestFactory => requestFactory.query().select(this.targetType());
    }

    self() {
        return this;
    }
}

export class SubscriptionRequest extends FilteringRequest {

    constructor(targetType, client) {
        super(targetType, client)
    }

    /**
     * @return {Promise<EntitySubscriptionObject<T extends Message>>}
     *
     * @template <T> a Protobuf type of entities being the target of a subscription
     */
    post() {
        const topic = this.builder().build();
        return this.client().subscribe(topic);
    }

    builderFn() {
        return requestFactory => requestFactory.topic().select(this.targetType());
    }

    self() {
        return this;
    }
}

const NOOP_CALLBACK = () => {};

export class CommandRequest {

    /**
     * @param {!Message} commandMessage
     * @param {!CommandingClient} client
     */
    constructor(commandMessage, client) {
        this._commandMessage = commandMessage;
        this._client = client;
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
     * @return {Promise<Map<Class<? extends Message>, EntitySubscriptionObject> | EntitySubscriptionObject>}
     */
    post() {
        // TODO:2019-11-27:dmytro.kuzmin:WIP Extend with event-subscribing logic.
        const ackCallback =
            {onOk: this._onAck, onError: this._onError, onRejection: this._onRejection};
        this._client.sendCommand(this._commandMessage, ackCallback, this._observedTypes);
    }
}
