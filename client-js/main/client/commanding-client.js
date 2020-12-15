/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import {Status} from "../proto/spine/core/response_pb";
import {CommandRequest} from "./command-request";
import {CommandHandlingError, CommandValidationError, SpineError} from "./errors";
import ObjectToProto from "./object-to-proto";
import {TypedMessage} from "./typed-message";

const _statusType = Status.typeUrl();

/**
 * A client which posts commands.
 *
 * This class has a default implementation but is intended to be overridden as necessary if it's
 * required to change the behavior.
 */
export class CommandingClient {

  constructor(endpoint, requestFactory) {
    this._requestFactory = requestFactory;
    this._endpoint = endpoint;
  }

  /**
   * Creates a new command request.
   *
   * @param {!Message} commandMessage the command to send to the server
   * @param {!Client} client the client which initiated the request
   * @return {CommandRequest} a new command request
   */
  command(commandMessage, client) {
    return new CommandRequest(commandMessage, client, this._requestFactory);
  }

  /**
   * Posts a given command to the Spine server.
   *
   * @param {!spine.core.Command} command a Command sent to Spine server
   * @param {!AckCallback} onAck a command acknowledgement callback
   */
  post(command, onAck) {
    const cmd = TypedMessage.of(command);
    this._endpoint.command(cmd)
        .then(ack => this._onAck(ack, onAck))
        .catch(error => {
          onAck.onError(new CommandHandlingError(error.message, error));
        });
  }

  _onAck(ack, onAck) {
    const responseStatus = ack.status;
    const responseStatusProto = ObjectToProto.convert(responseStatus, _statusType);
    const responseStatusCase = responseStatusProto.getStatusCase();

    switch (responseStatusCase) {
      case Status.StatusCase.OK:
        onAck.onOk();
        break;
      case Status.StatusCase.ERROR:
        const error = responseStatusProto.getError();
        const message = error.getMessage();
        onAck.onError(error.hasValidationError()
            ? new CommandValidationError(message, error)
            : new CommandHandlingError(message, error));
        break;
      case Status.StatusCase.REJECTION:
        onAck.onImmediateRejection(responseStatusProto.getRejection());
        break;
      default:
        onAck.onError(
            new SpineError(`Unknown response status case ${responseStatusCase}`)
        );
    }
  }
}
