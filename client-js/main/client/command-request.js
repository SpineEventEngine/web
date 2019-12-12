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

import {CommandId} from "../proto/spine/core/command_pb";
import {MessageId, Origin} from "../proto/spine/core/diagnostics_pb";
import {Filters} from "./actor-request-factory";
import {AnyPacker} from "./any-packer";
import {ClientRequest} from "./client-request";
import {Type} from "./typed-message";

const NOOP_CALLBACK = () => {};

/**
 * A request to post a command to the Spine backend.
 *
 * Optionally allows to subscribe to events that are the immediate results of handling the command.
 *
 * A usage example:
 * ```
 * client.command(logInUser)
 *       .onOk(_logOk)
 *       .onError(_logError)
 *       .observe(UserLoggedIn.class, ({subscribe, unsubscribe}) => {
 *           subscribe(event => _logAndUnsubscribe(event, unsubscribe));
 *           setTimeout(unsubscribe, EVENT_WAIT_TIMEOUT);
 *       })
 *       .observe(UserAlreadyLoggedIn.class, (({subscribe, unsubscribe}) => {
 *           subscribe(event => _warnAboutAndUnsubscribe(event, unsubscribe));
 *           setTimeout(unsubscribe, EVENT_WAIT_TIMEOUT);
 *       })
 *       .post();
 * ```
 *
 * The `subscribe` callback provided to the consumer allows to configure an event receival process
 * while the `unsubscribe` callback allows to cancel the subscription.
 *
 * Please note that in the example above we make sure the subscription is cancelled even if the
 * specified event type is never received.
 */
export class CommandRequest extends ClientRequest {

  /**
   * @param {!Message} commandMessage the command to post
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
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
   * Runs the callback if the command is successfully handled by the Spine server.
   *
   * @param {!parameterlessCallback} callback the callback to run
   * @return {this} self for method chaining
   */
  onOk(callback) {
    this._onAck = callback;
    return this;
  }

  /**
   * Runs the callback if the command could not be handled by the Spine server due to the
   * technical error.
   *
   * @param {!consumerCallback<CommandHandlingError>} callback the callback to run
   * @return {this} self for method chaining
   */
  onError(callback) {
    this._onError = callback;
    return this;
  }

  /**
   * Runs the callback if the server responded with the `rejection` status on a command.
   *
   * Note that with the current Spine server implementation it's rare for the command to be
   * rejected right away. In most cases, the command will be acknowledged with the `OK` status and
   * only then lead to a business rejection. You can check this scenario using the `observe` method.
   *
   * @param {!consumerCallback<spine.core.Event>} callback
   * @return {this} self for method chaining
   */
  onRejection(callback) {
    this._onRejection = callback;
    return this;
  }

  /**
   * Adds the event type to the list of observed command handling results.
   *
   * @param {!Class<? extends Message>} eventType a type of the observed events
   * @param {!consumerCallback<EventSubscriptionCallbacks>} consumer
   *        a consumer of the `subscribe` and `unsubscribe` callbacks which are responsible for
   *        accepting the incoming events and cancelling the subscription respectively
   * @return {this} self for method chaining
   */
  observe(eventType, consumer) {
    this._observedTypes.push({type: eventType, consumer: consumer});
    return this;
  }

  /**
   * Posts the command to the server and subscribes to all observed types.
   *
   * @return {Promise<void>} a promise that signals if the command posting was done successfully,
   *                         may be ignored
   */
  post() {
    const command = this._requestFactory.command().create(this._commandMessage);
    const onAck = {onOk: this._onAck, onError: this._onError, onRejection: this._onRejection};
    const promises = [];
    this._observedTypes.forEach(({type, consumer}) => {
      const originFilter = Filters.eq("context.past_message", this._asOrigin(command));
      const promise = this._client.subscribeToEvent(type)
          .where(originFilter)
          .post()
          .then(({eventEmitted, unsubscribe}) => {
            const subscribe = eventConsumer => {
              eventEmitted.subscribe({
                next: eventConsumer
              });
            };
            consumer({subscribe, unsubscribe});
          });
      promises.push(promise);
    });
    const subscriptionPromise = Promise.all(promises);
    return subscriptionPromise.then(() => this._client.post(command, onAck));
  }

  /**
   * @param {!Command} command
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
