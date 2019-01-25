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

import {Type, TypedMessage} from './typed-message';
import {CommandHandlingError, CommandValidationError, SpineError} from './errors';
import TypeParsers from './parser/type-parsers';
import KnownTypes from './known-types';
import {Status} from '../proto/spine/core/response_pb';
import {BackendClient} from './backend-client';

/**
 * A utility which converts the JS object to its Protobuf counterpart.
 */
class ObjectToProto {

  /**
   * Converts the object to the corresponding Protobuf message.
   *
   * The input object is supposed to be a Protobuf message representation, i.e. all its attributes should correspond to
   * the fields of the specified message type.
   *
   * @param {Object} object an object to convert
   * @param {Type} type a type of the corresponding Protobuf message
   */
  static convert(object, type) {
    const typeUrl = type.url().value();
    const parser = TypeParsers.parserFor(typeUrl);
    const proto = parser.fromObject(object);
    return proto;
  }
}

/**
 * Matches the static information about the query with the queried entity type gathered at runtime.
 */
class TypedQuery {

  /**
   * @param {!spine.client.Query} query an underlying proto message representing the query
   * @param {!Type} type a type of the entity this query is targeted on
   */
  constructor(query, type) {
    this._query = query;
    this._type = type;
  }

  /**
   * Retrieves the underlying proto message.
   */
  raw() {
    return this._query;
  }

  /**
   * Converts a query response sent by the server to the corresponding proto message.
   *
   * @param {!Object} response an object representing the response for the query
   */
  convert(response) {
    return ObjectToProto.convert(response, this._type);
  }
}

/**
 * Matches the static information about the topic with the subscribed entity type gathered at runtime.
 */
class TypedTopic {

  /**
   * @param {!spine.client.Topic} topic an underlying proto message representing the topic
   * @param {!Type} type a type of the subscription target
   */
  constructor(topic, type) {
    this._topic = topic;
    this._type = type;
  }

  /**
   * Retrieves the underlying proto message.
   */
  raw() {
    return this._topic;
  }

  /**
   * Converts an object which represents the observed entity update to the corresponding proto message.
   *
   * @param {!Object} update an object representing the entity update
   */
  convert(update) {
    return ObjectToProto.convert(update, this._type);
  }
}

/**
 * A mediate abstract `BackendClient` for Spine application backend. Defines operations that client is able
 * to perform (`.fetchAll(...)`, `.sendCommand(...)`, etc.) without reference to the particular data provider.
 *
 * @abstract
 */
export class AbstractBackendClient extends BackendClient {

  /**
   * @param {!Endpoint} endpoint an endpoint to send requests to
   * @param {!ActorRequestFactory} actorRequestFactory
   *        a request factory to build requests to Spine server
   */
  constructor(endpoint, actorRequestFactory) {
    super();
    this._endpoint = endpoint;
    this._requestFactory = actorRequestFactory;
  }

  /**
   * @inheritDoc
   */
  fetchAll({ofType: type}) {
    const query = this._requestFactory.query().select(type).build();
    const typedQuery = new TypedQuery(query, type);
    return this._fetchOf(typedQuery);
  }

  /**
   * @inheritDoc
   */
  fetchById(type, id, dataCallback, errorCallback) {
    const typedId = TypedMessage.of(id);
    const query = this._requestFactory.query().select(type).byIds([typedId]).build();
    const typedQuery = new TypedQuery(query, type);

    let itemReceived = false;

    const observer = {
      next: item => {
        itemReceived = true;
        dataCallback(item);
      },
      complete: () => {
        if (!itemReceived) {
          dataCallback(null);
        }
      }
    };

    if (errorCallback) {
      observer.error = errorCallback;
    }
    this._fetchOf(typedQuery).oneByOne().subscribe(observer);
  }

  /**
   * @inheritDoc
   */
  sendCommand(commandMessage, acknowledgedCallback, errorCallback, rejectionCallback) {
    const command = this._requestFactory.command().create(commandMessage);
    this._endpoint.command(command)
      .then(ack => {
        const responseStatus = ack.status;
        const responseStatusProto = ObjectToProto.convert(responseStatus, Type.forClass(Status));
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
            errorCallback(new SpineError(`Unknown response status case ${responseStatusCase}`));
        }
      })
      .catch(error => {
        errorCallback(new CommandHandlingError(error.message, error));
      });
  }

  /**
   * @inheritDoc
   */
  subscribeToEntities({ofType: type, byIds: ids, byId: id}) {
    if (typeof ids !== 'undefined' && typeof id !== 'undefined') {
      throw new Error('You can specify only one of ids or id as a parameter to subscribeToEntities');
    }
    if (typeof id !== 'undefined') {
      ids = [id];
    }
    let topic;
    if (ids) {
      const typedIds = ids.map(TypedMessage.of);
      topic = this._requestFactory.topic().all({of: type, withIds: typedIds});
    } else {
      topic = this._requestFactory.topic().all({of: type});
    }
    const typedTopic = new TypedTopic(topic, type);
    return this._subscribeToTopic(typedTopic);
  }

  /**
   * Registers all Protobuf types provided by the specified modules.
   *
   * <p>After the registration, the types can be used and parsed correctly.
   *
   * @example
   * import * as protobufs from './proto/index.js';
   * BackendClient.registerTypes(protobufs);
   *
   * @param protoIndexFiles the index.js files generated by
   * {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
   */
  static registerTypes(...protoIndexFiles) {
    for (let indexFile of protoIndexFiles) {
      this._registerTypes(indexFile);
    }
    return this;
  }

  static _registerTypes(indexFile) {
    for (let [typeUrl, type] of indexFile.types) {
      KnownTypes.register(type, typeUrl);
    }
    for (let [typeUrl, parserType] of indexFile.parsers) {
      TypeParsers.register(new parserType(), typeUrl);
    }
  }

  /**
   * Creates a new Fetch object specifying the target of fetch and its parameters.
   *
   * @param {!TypedQuery} query a typed query which contains runtime information about the queried entity type
   * @return {BackendClient.Fetch<T>} an object that performs the fetch
   * @template <T> type of Fetch results
   * @protected
   * @abstract
   */
  _fetchOf(query) {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Creates a subscription to the topic which is updated with backend changes.
   *
   * @param {!TypedTopic} topic a typed topic which contains runtime information about the subscribed entity type
   * @return {Promise<EntitySubscriptionObject>}
   * @protected
   * @abstract
   */
  _subscribeToTopic(topic) {
    throw new Error('Not implemented in abstract base.');
  }
}
