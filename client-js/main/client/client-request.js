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
import {Observable} from 'rxjs';

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
}

export class CommandRequest {

    /**
     * @param {!Message} commandMessage
     * @param {!CommandingClient} client
     */
    constructor(commandMessage, client) {
        this._commandMessage = commandMessage;
        this._client = client;
    }

    /**
     * @return {Promise<Map<T extends Message, Observable> | Observable>}
     *
     * @template <T> a Protobuf type of entities being the target of a query
     */
    post() {
        // TODO:2019-11-27:dmytro.kuzmin:WIP Extend with event-subscribing logic.
        this._client.sendCommand(this._commandMessage);
    }
}

export class QueryRequest extends FilteringRequest {

    constructor(targetType, client) {
        super(targetType, client)
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
}
