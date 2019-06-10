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

import {CommandHandlingError, CommandValidationError, SpineError} from './errors';
import KnownTypes from './known-types';
import ObjectToProto from './object-to-proto';
import {Status} from '../proto/spine/core/response_pb';
import {Client} from './client';

/**
 * A mediate abstract `Client` for Spine application backend.
 *
 * Defines operations that client is able to perform (`.fetch(...)`, `.sendCommand(...)`, etc.)
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
  newQuery() {
    return this._requestFactory.query();
  }

  /**
   * @inheritDoc
   */
  newTopic() {
    return this._requestFactory.topic();
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
  fetch({entity: cls, byIds: ids}) {
    const queryBuilder = this.newQuery().select(cls);
    const query = AbstractClient._buildTarget(queryBuilder, ids);
    return this.execute(query);
  }

  /**
   * @inheritDoc
   */
  subscribe({entity: cls, byIds: ids}) {
    const topicBuilder = this.newTopic().select(cls);
    const topic = AbstractClient._buildTarget(topicBuilder, ids);

    return this.subscribeTo(topic);
  }

  /**
   * Builds target from the given target builder specifying the set
   * of target entities.
   *
   * The built target points to:
   *  - the particular entities with the given IDs;
   *  - the all entities if no IDs specified;
   *
   * @param {AbstractTargetBuilder<Query|Topic>} targetBuilder
   *    a builder for creating `Query` or `Topic` instances.
   * @param {?<T extends Message>[] | <T extends Message> | Number[] | Number | String[] | String} ids
   *      a list of target entities IDs or an ID of a single target entity
   * @return {Query|Topic} the built target
   *
   * @template <T> a class of a query or subscription target entities identifiers
   * @private
   */
  static _buildTarget(targetBuilder, ids) {
    if (Array.isArray(ids)) {
      targetBuilder.byIds(ids);
    } else if (!!ids) {
      targetBuilder.byIds([ids]);
    }

    return targetBuilder.build();
  }
}
