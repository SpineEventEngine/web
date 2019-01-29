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

import {TypedMessage} from './typed-message';
import {CommandHandlingError, CommandValidationError, SpineError} from './errors';
import KnownTypes from './known-types';
import ObjectToProto from './object-to-proto';
import {Status} from '../proto/spine/core/response_pb';
import {Client} from './client';

/**
 * A mediate abstract `Client` for Spine application backend.
 *
 * Defines operations that client is able to perform (`.fetchAll(...)`, `.sendCommand(...)`, etc.)
 * without reference to the particular data provider.
 *
 * @abstract
 */
export class AbstractClient extends Client {

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
    return this._fetchOf(query);
  }

  /**
   * @inheritDoc
   */
  fetchById(type, id, dataCallback, errorCallback) {
    const typedId = TypedMessage.of(id);
    const query = this._requestFactory.query().select(type).byIds([typedId]).build();

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
    this._fetchOf(query).oneByOne().subscribe(observer);
  }

  /**
   * @inheritDoc
   */
  sendCommand(commandMessage, acknowledgedCallback, errorCallback, rejectionCallback) {
    const command = this._requestFactory.command().create(commandMessage);
    this._endpoint.command(command)
      .then(ack => {
        const responseStatus = ack.status;
        const responseStatusProto = ObjectToProto.convert(responseStatus, KnownTypes.typeUrlFor(Status));
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
      throw new Error('No entity IDs set. Specify either a single entity ID or' +
          ' multiple entity IDs to subscribe to the entity state updates.');

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
    return this._subscribeTo(topic);
  }

  /**
   * Creates a new `Fetch` object specifying the target of fetch and its parameters.
   *
   * @param {!spine.client.Query} query a request to the read-side
   * @return {Client.Fetch<T>} an object that performs the fetch
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
   * @param {!spine.client.Topic} topic a topic of a subscription
   * @return {Promise<EntitySubscriptionObject>}
   * @protected
   * @abstract
   */
  _subscribeTo(topic) {
    throw new Error('Not implemented in abstract base.');
  }
}
