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
 * @typedef EventSubscriptionCallbacks
 *
 * A pair of callbacks that allow to add an event consumer and cancel the subscription
 * respectively.
 *
 * @property {consumerCallback<eventConsumer>} subscribe the callback which allows to setup an
 *                                                       event consumer to use for the subscription
 * @property {parameterlessCallback} unsubscribe the callback which allows to cancel the
 *                                               subscription
 */

/**
 * A request from a client to the Spine backend.
 *
 * @abstract
 */
class ClientRequest {

  /**
   * @protected
   */
  constructor(client, requestFactory) {

    /**
     * @type Client
     *
     * @protected
     */
    this._client = client;

    /**
     * @type ActorRequestFactory
     *
     * @protected
     */
    this._requestFactory = requestFactory;
  }
}

/**
 * An abstract base for client requests that filter messages by certain criteria.
 *
 * @abstract
 *
 * @template <B> the type of the builder wrapped by this request
 * @template <T> the type of the messages that store the request data
 */
class FilteringRequest extends ClientRequest {

  /**
   * @param {!Class<? extends Message>} targetType the target type of the request
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
   *
   * @protected
   */
  constructor(targetType, client, actorRequestFactory) {
    super(client, actorRequestFactory);
    this.targetType = targetType;
  }

  /**
   * Adds filtering by IDs to the built request.
   *
   * @param ids {!<? extends Message>|Number|String|<? extends Message>[]|Number[]|String[]}
   *        the IDs of interest
   * @return {this} self for method chaining
   */
  byId(ids) {
    ids = FilteringRequest._ensureArray(ids);
    this._builder().byIds(ids);
    return this._self();
  }

  /**
   * Adds filtering by predicates to the built request.
   *
   * Filters specified in a list are considered to be joined using `AND` operator.
   *
   * @param {!Filter|CompositeFilter|Filter[]|CompositeFilter[]} predicates the filters
   * @return {this} self for method chaining
   */
  where(predicates) {
    predicates = FilteringRequest._ensureArray(predicates);
    this._builder().where(predicates);
    return this._self();
  }

  /**
   * Applies a field mask to the request results.
   *
   * The names of the fields must be formatted according to the `google.protobuf.FieldMask`
   * specification.
   *
   * @param {!String|String[]} fieldPaths the fields to include in the mask
   * @return {this} self for method chaining
   */
  withMask(fieldPaths) {
    fieldPaths = FilteringRequest._ensureArray(fieldPaths);
    this._builder().withMask(fieldPaths);
    return this._self();
  }

  /**
   * Returns the builder for messages that store request data.
   *
   * @return {AbstractTargetBuilder<T extends Message>} the builder instance
   *
   * @protected
   */
  _builder() {
    if (!this._builderInstance) {
      const newBuilderFn = this._newBuilderFn();
      this._builderInstance = newBuilderFn(this._requestFactory);
    }
    return this._builderInstance;
  }

  /**
   * Returns the function with which the {@link _builderInstance} can be created.
   *
   * @abstract
   * @return {Function<ActorRequestFactory, B extends AbstractTargetBuilder>}
   *
   * @protected
   */
  _newBuilderFn() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * @abstract
   * @return {this}
   *
   * @protected
   */
  _self() {
    throw new Error('Not implemented in abstract base.');
  }

  /**
   * Wraps the passed argument into array if it's not an array already.
   *
   * The `null` values are converted into an empty array.
   *
   * @return {Array} the passed argument as an array
   *
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

/**
 * A request to retrieve entities of the given type.
 *
 * Allows to post the query data to the Spine backend and receive the entity states as `Promise`.
 *
 * A usage example:
 * ```
 * const customers =
 *          client.select(Customer.class)
 *                .byId(westCoastCustomerIds())
 *                .withMask("name", "address", "email")
 *                .where([Filters.eq("type", "permanent"),
 *                       Filters.eq("discount_percent", 10),
 *                       Filters.eq("company_size", Company.Size.SMALL)])
 *                .orderBy("name", OrderBy.Direction.ASCENDING)
 *                .limit(20)
 *                .run(); // The returned type is `Promise<Customer[]>`.
 * ```
 *
 * All of the called filtering methods are optional. If none of them are specified, all entities
 * of type will be retrieved.
 *
 * @template <T> the query target type
 */
export class QueryRequest extends FilteringRequest {

  /**
   * @param {!Class<T extends Message>} targetType the target type of entities
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
   */
  constructor(targetType, client, actorRequestFactory) {
    super(targetType, client, actorRequestFactory)
  }

  /**
   * Sets the sorting order for the retrieved results.
   *
   * @param {!String} column the column to order by
   * @param {!OrderBy.Direction} direction the ascending/descending direction
   * @return {this} self for method chaining
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
   * Sets the maximum number of returned entities.
   *
   * Can only be used in conjunction with the {@link #orderBy} condition.
   *
   * @param {number} count the max number of response entities
   * @return {this} self for method chaining
   */
  limit(count) {
    this._builder().limit(count);
    return this._self();
  }

  /**
   * Builds a `Query` instance based on currently specified filters.
   *
   * @return {spine.client.Query} a `Query` instance
   */
  query() {
    return this._builder().build();
  }

  /**
   * Runs the query and obtains the results.
   *
   * @return {Promise<<T extends Message>[]>} the asynchronously resolved query results
   */
  run() {
    const query = this.query();
    return this._client.read(query);
  }

  /**
   * @inheritDoc
   */
  _newBuilderFn() {
    return requestFactory => requestFactory.query().select(this.targetType);
  }

  /**
   * @inheritDoc
   */
  _self() {
    return this;
  }
}

/**
 * An abstract base for requests that subscribe to messages of a certain type.
 *
 * @abstract
 * @template <T> the target type of messages, for events the type is always `spine.core.Event`
 */
class SubscribingRequest extends FilteringRequest {

  /**
   * Builds a `Topic` instance based on the currently specified filters.
   *
   * @return {spine.client.Topic} a `Topic` instance
   */
  topic() {
    return this._builder().build();
  }

  /**
   * Posts a subscription request and returns the result as `Promise`.
   *
   * @return {Promise<EntitySubscriptionObject<T extends Message> | EventSubscriptionObject>}
   *         the asynchronously resolved subscription object
   */
  post() {
    const topic = this.topic();
    return this._subscribe(topic);
  }

  /**
   * @inheritDoc
   */
  _newBuilderFn() {
    return requestFactory => requestFactory.topic().select(this.targetType);
  }

  /**
   * @abstract
   * @return {Promise<EntitySubscriptionObject<T extends Message> | EventSubscriptionObject>}
   *
   * @protected
   */
  _subscribe(topic) {
    throw new Error('Not implemented in abstract base.');
  }
}

/**
 * A request to subscribe to updates of entity states of a certain type.
 *
 * Allows to obtain the `EntitySubscriptionObject` which exposes the entity changes in a form of
 * callbacks which can be subscribed to.
 *
 * A usage example:
 * ```
 * client.subscribeTo(Task.class)
 *       .where(Filters.eq("status", Task.Status.ACTIVE))
 *       // Additional filtering can be done here.
 *       .post()
 *       .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
 *           itemAdded.subscribe(_addDisplayedTask);
 *           itemChanged.subscribe(_changeDisplayedTask);
 *           itemRemoved.subscribe(_removeDisplayedTask);
 *       });
 * ```
 *
 * If the entity matched the subscription criteria at one point, but stopped to do so, the
 * `itemRemoved` callback will be triggered for it. The callback will contain the last entity state
 * that matched the subscription.
 *
 * Please note that the subscription object should be manually unsubscribed when it's no longer
 * needed to receive the updates. This can be done with the help of `unsubscribe` callback.
 *
 * @template <T> the target entity type
 */
export class SubscriptionRequest extends SubscribingRequest {

  /**
   * @param {!Class<T extends Message>} entityType the target entity type
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
   */
  constructor(entityType, client, actorRequestFactory) {
    super(entityType, client, actorRequestFactory)
  }

  /**
   * @inheritDoc
   *
   * @return {Promise<EntitySubscriptionObject<T extends Message>>}
   */
  _subscribe(topic) {
    return this._client.subscribe(topic);
  }

  /**
   * @inheritDoc
   */
  _self() {
    return this;
  }
}

/**
 * A request to subscribe to events of a certain type.
 *
 * Allows to obtain the `EventSubscriptionObject` which reflects the events that happened in the
 * system and match the subscription criteria.
 *
 * A usage example:
 * ```
 * client.subscribeToEvent(TaskCreated.class)
 *       .where([Filters.eq("task_priority", Task.Priority.HIGH),
 *              Filters.eq("context.past_message.actor_context.actor", userId)])
 *       .post()
 *       .then(({eventEmitted, unsubscribe}) => {
 *           eventEmitted.subscribe(_logEvent);
 *       });
 * ```
 *
 * The fields specified to the `where` filters should either be a part of the event message or
 * have a `context.` prefix and address one of the fields of the `EventContext` type.
 *
 * The `eventEmitted` observable reflects all events that occurred in the system and match the
 * subscription criteria, in a form of `spine.core.Event`.
 *
 * Please note that the subscription object should be manually unsubscribed when it's no longer
 * needed to receive the updates. This can be done with the help of `unsubscribe` callback.
 */
export class EventSubscriptionRequest extends SubscribingRequest {

  /**
   * @param {!Class<? extends Message>} eventType the target event type
   * @param {!Client} client the client which initiated the request
   * @param {!ActorRequestFactory} actorRequestFactory the request factory
   */
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
   */
  _self() {
    return this;
  }
}

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
   * Note that with the current Spine server implementation the command being rejected right away
   * is very unlikely. In most cases, the command will be acknowledged with `OK` status and only
   * then lead to a business rejection. You can check this scenario using the `observe` method.
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
