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

/**
 * The callback that doesn't accept any parameters.
 * @callback parameterlessCallback
 */

/**
 * The callback that accepts single parameter.
 *
 * @callback consumerCallback
 * @param {T} the value the callback function accepts
 *
 * @template <T> the type of value passed to the callback
 */

/**
 * @typedef {Object} EntitySubscriptionObject
 *
 * An object representing a result of the subscription to entities state changes.
 * The entities that already exist will be initially passed to the `itemAdded` observer.
 *
 * @property {!Observable<T>} itemAdded emits new items matching the subscription topic
 * @property {!Observable<T>} itemChanged emits updated items matching the subscription topic
 * @property {!Observable<T>} itemRemoved emits removed items matching the subscription topic
 * @property {!parameterlessCallback} unsubscribe a method to be called to cancel the subscription,
 *                                   stopping the subscribers from receiving new entities
 *
 * @template <T> a type of the subscription target entities
 */

/**
 * @typedef {Object} SimpleTarget
 *
 * An object representing a set of parameters for building a query or a subscription
 * topic specifying only a type and identifiers of the target entities.
 *
 * Target built from this object point either:
 *  - a single entity of a given type with a given ID;
 *  - several entities of a given type with given IDs;
 *  - all entities of a given type if no IDs specified;
 *
 * @property {!Class<T extends Message>} entity a class of target entities
 * @property {?<I extends Message>[] | <I extends Message> | Number[] | Number | String[] | String} byIds
 *      a list of target entities IDs or an ID of a single target entity
 *
 * @template <T> a class of a query or subscription target entities
 * @template <I> a class of a query or subscription target entities identifiers
 */

/**
 * An abstract client for Spine application backend. This is a single channel for client-server
 * communication in a Spine-based browser application.
 *
 * @abstract
 */
export class Client {

  /**
   * @param {!Class<? extends Message>} entityType a Protobuf type of the query target entities
   * @return {QueryRequest}
   */
  select(entityType) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!Class<? extends Message>} type a Protobuf type of the target entities or events
   * @return {SubscriptionRequest}
   */
  subscribeTo(type) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @param {!Message} command a Protobuf type of the query target entities
   * @return {CommandRequest}
   */
  command(command) {
    throw new Error('Not implemented in abstract base.');
  }
}
