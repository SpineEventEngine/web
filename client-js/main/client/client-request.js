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
import {Command, CommandId} from '../proto/spine/core/command_pb';
import {MessageId, Origin} from '../proto/spine/core/diagnostics_pb';
import {AnyPacker} from "./any-packer";
import {Filters} from "./actor-request-factory";
import {Type} from "./typed-message";

/**
 * @abstract
 */
class ClientRequest {

    /**
     * @param {!Client} client
     * @param {!ActorRequestFactory} requestFactory
     *
     * @protected
     */
    constructor(client, requestFactory) {

        /**
         * @protected
         */
        this._client = client;

        /**
         * @protected
         */
        this._requestFactory = requestFactory;
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
     *
     * @protected
     */
    constructor(targetType, client, actorRequestFactory) {
        super(client, actorRequestFactory);
        this.targetType = targetType;
    }

    /**
     * @param ids {!<I extends Message>|Number|String|<I extends Message>[]|Number[]|String[]}
     * @return {this} self for method chaining
     *
     * @template <I> a Protobuf type of IDs
     */
    byId(ids) {
        ids = FilteringRequest._ensureArray(ids);
        this._builder().byIds(ids);
        return this._self();
    }

    /**
     * ...
     *
     * The subsequent calls override each other.
     *
     * @param {!Filter|CompositeFilter|Filter[]|CompositeFilter[]} predicates
     * @return {this} self for method chaining
     */
    where(predicates) {
        predicates = FilteringRequest._ensureArray(predicates);
        this._builder().where(predicates);
        return this._self();
    }

    /**
     * @param {!String|String[]} fieldNames
     * @return {this} self for method chaining
     */
    withMask(fieldNames) {
        fieldNames = FilteringRequest._ensureArray(fieldNames);
        this._builder().withMask(fieldNames);
        return this._self();
    }

    /**
     * @return {AbstractTargetBuilder<T extends Message>}
     *
     * @template <T>
     *
     * @protected
     */
    _builder() {
        // TODO:2019-11-27:dmytro.kuzmin:WIP Check that setting to some initial value is
        //  unnecessary.
        if (!this._builderInstance) {
            this._builderInstance = this._newBuilderFn()(this._requestFactory);
        }
        return this._builderInstance;
    }

    /**
     * @abstract
     *
     * @return {Function<ActorRequestFactory, T extends AbstractTargetBuilder>}
     *
     * @template <T>
     *
     * @protected
     */
    _newBuilderFn() {
        throw new Error('Not implemented in abstract base.');
    }

    /**
     * @abstract
     *
     * @return {this}
     *
     * @protected
     */
    _self() {
        throw new Error('Not implemented in abstract base.');
    }

    /**
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

export class QueryRequest extends FilteringRequest {

    constructor(targetType, client, actorRequestFactory) {
        super(targetType, client, actorRequestFactory)
    }

    /**
     *
     * @param {!String} column
     * @param {!OrderBy.Direction} direction
     * @return {QueryRequest} self
     */
    orderBy(column, direction) {
        if (direction === OrderBy.Direction.ASCENDING) {
            this._builder().orderAscendingBy(column);
        } else {
            this._builder().orderDescendingBy(column);
        }
        return this._self();
    }

    /**
     * @param {number} count the max number of response entities
     * @return {QueryRequest} self
     */
    limit(count) {
        this._builder().limit(count);
        return this._self();
    }

    /**
     * @return {spine.client.Query}
     */
    query() {
        return this._builder().build();
    }

    /**
     * @return {Promise<<T extends Message>[]>}
     *
     * @template <T> a Protobuf type of entities being the target of a query
     */
    run() {
        const query = this.query();
        return this._client.read(query);
    }

    /**
     * @inheritDoc
     *
     * @return {Function<ActorRequestFactory, QueryBuilder>}
     */
    _newBuilderFn() {
        return requestFactory => requestFactory.query().select(this.targetType);
    }

    /**
     * @inheritDoc
     *
     * @return {QueryRequest}
     */
    _self() {
        return this;
    }
}

/**
 * @abstract
 */
class SubscribingRequest extends FilteringRequest {

    topic() {
        return this._builder().build();
    }

    /**
     * @return {Promise<EntitySubscriptionObject<T extends Message> | EventSubscriptionObject>}
     *
     * @template <T> a Protobuf type of entities being the target of a subscription
     */
    post() {
        const topic = this.topic();
        return this._subscribe(topic);
    }

    /**
     * @inheritDoc
     *
     * @return {Function<ActorRequestFactory, TopicBuilder>}
     */
    _newBuilderFn() {
        return requestFactory => requestFactory.topic().select(this.targetType);
    }

    /**
     * @abstract
     *
     * @return {Promise<EntitySubscriptionObject<T extends Message> | EventSubscriptionObject>}
     *
     * @template <T> a Protobuf type of entities being the target of a subscription
     *
     * @protected
     */
    _subscribe(topic) {
        throw new Error('Not implemented in abstract base.');
    }
}

export class SubscriptionRequest extends SubscribingRequest {

    constructor(entityType, client, actorRequestFactory) {
        super(entityType, client, actorRequestFactory)
    }

    /**
     * @inheritDoc
     *
     * @return {Promise<EntitySubscriptionObject<T extends Message>>}
     *
     * @template <T>
     */
    _subscribe(topic) {
        return this._client.subscribe(topic);
    }

    /**
     * @inheritDoc
     *
     * @return {SubscriptionRequest}
     */
    _self() {
        return this;
    }
}

export class EventSubscriptionRequest extends SubscribingRequest {

    constructor(eventType, client, actorRequestFactory) {
        super(eventType, client, actorRequestFactory)
    }

    /**
     * @inheritDoc
     *
     * @return {Promise<EventSubscriptionObject>}
     */
    _subscribe(topic) {
        return this._client.subscribeToEvents(topic);
    }

    /**
     * @inheritDoc
     *
     * @return {EventSubscriptionRequest}
     */
    _self() {
        return this;
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
    onOk(callback) {
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
     * @return {Promise<EventSubscriptionObject[] | EventSubscriptionObject>}
     */
    post() {
        const command = this._requestFactory.command().create(this._commandMessage);
        const onAck =
            {onOk: this._onAck, onError: this._onError, onRejection: this._onRejection};
        const promises = [];
        this._observedTypes.forEach(type => {
            const originFilter = Filters.eq("context.past_message", this._asOrigin(command));
            const promise = this._client.subscribeToEvent(type)
                .where(originFilter)
                .post();
            promises.push(promise);
        });
        const subscriptionPromise = promises.length === 1
            ? promises[0]
            : Promise.all(promises);

        // noinspection JSValidateTypes the types are actually correct.
        return subscriptionPromise.then((subscriptionObject) => {
            this._client.post(command, onAck);
            return subscriptionObject;
        });
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
        const commandIdType = Type.forClass(CommandId);
        const packedId = AnyPacker.pack(command.getId()).as(commandIdType);
        messageId.setId(packedId);
        const typeUrl = command.getMessage().getTypeUrl();
        messageId.setTypeUrl(typeUrl);
        result.setMessage(messageId);

        let grandOrigin = command.getContext().getOrigin();
        if (!grandOrigin) {
            grandOrigin = new Origin();
        }
        result.setGrandOrigin(grandOrigin);

        const actorContext = command.getContext().getActorContext();
        result.setActorContext(actorContext);
        return result;
    }
}
