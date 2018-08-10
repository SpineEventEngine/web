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
/**
 * @callback nextCallback
 * @param {*} data
 */

/**
 * @callback errorCallback
 * @param {Object} error
 */

/**
 * @callback completeCallback
 */

/**
 * @callback observableInput
 * @param {Observer} observer
 */

/**
 * An abstract Observer class.
 *
 * It is passed new values from the Observable to its {@code #next(value)} method,
 * errors to {@code #error(err)} method and a notification about Observable completion to its
 * {@code complete()} method.
 *
 * This class defines an interface for documentation and code completion. In real code use
 * a Javascript Object instead:
 * <code>
 *   observable.subscribe({
 *     next(value) {...},
 *     error(err) {...},
 *     complete() {...} 
 *   })
 * </code>
 *
 */
class Observer {
  // noinspection JSMethodCanBeStatic
  /**
   * Invoked by the Observable when it retrieves a new value.
   *
   * @param value {*}
   */
  next(value) {
    throw new Error("Unimplemented by an abstract Observer");
  }

  // noinspection JSMethodCanBeStatic
  /**
   * Invoked by the Observable an error occures while processing its values.
   *
   * @param err {Error}
   */
  error(err) {
    throw new Error("Unimplemented by an abstract Observer");
  }

  // noinspection JSMethodCanBeStatic
  /**
   * Invoked by the Observable when its complete.
   */
  complete() {
    throw new Error("Unimplemented by an abstract Observer");
  }
}

/**
 * A subscription returned by the Observable allowing to unsubscribe the Observer from receiving
 * new values.
 */
export class Subscription {

  constructor(unsubscribe) {
    this._unsubscribe = unsubscribe;
  }

  /**
   * Unsubscribes the Observer from Observable stopping it from receiving new values.
   */
  unsubscribe() {
    this._unsubscribe();
  }
}

/**
 * An Observable Subscriber sending off received values, errors and complete notifications to the
 * observer.
 */
class Subscriber {

  constructor(destination) {
    this.destination = destination;
  }

  /**
   * Sends off the next Observable value to the Observer, skipping it if the Observable was
   * unsubscribed or the Observer was complete.
   * @param value {*} a next value for the Observer
   */
  next(value) {
    if (!this.isStopped) {
      this.destination.next(value);
    }
  }

  /**
   * Sends off an error to the Observer, stopping it from receiving further values.
   * @param err {Error} an error to be passed to the Observer
   */
  error(err) {
    if (!this.isStopped) {
      this.isStopped = true;
      this.destination.error(err);
      this.unsubscribe();
    }
  }

  /**
   * Sends off a complete notification to the Observer, stopping it from receiving further values.
   */
  complete() {
    if (!this.isStopped) {
      this.isStopped = true;
      this.destination.complete();
      this.unsubscribe();
    }
  }

  /**
   * Stops the Subscriber from sending new messages to the Observer.
   */
  unsubscribe() {
    this.isStopped = true;
  }

  /**
   * Creates a new Subscriber from a provided Observer, supplying it with default next, error
   * and complete method implementations.
   *
   * {@code next} and {@code complete} use no-op as a default, while the {@code error} is logging
   * the values to the console.
   *
   * @param next
   * @param error
   * @param complete
   * @return {Subscriber}
   */
  static fromObservable(next, error, complete) {
    let _next = next;
    if (this.isUndefined(_next)) {
      _next = this._noop;
    }
    let _error = error;
    if (this.isUndefined(_error)) {
      _error = this._consoleErrorHandler;
    }
    let _complete = complete;
    if (this.isUndefined(_complete)) {
      _complete = this._noop;
    }

    const observer = {next: _next, error: _error, complete: _complete};

    return new Subscriber(observer);
  }

  /**
   * A no-operation function that accepts any arguments, return undefined and does nothing.
   * @private
   */
  static _noop() {
    // Does nothing.
  }

  /**
   * A default error handler used by Observable, logging the error to console.
   * @private
   */
  static _consoleErrorHandler(error) {
    console.error(error);
  }

  static isUndefined(value) {
    return typeof value === "undefined";
  }
}

/**
 * An implementation of Observable pattern.
 *
 * An Observable represents a set of values over some period of time.
 *
 * An Observable accepts a single subscriber, supplying each new observed value to its
 * {@code next(item)} method.
 *
 * When all of the possible values are observed an Observable call Observers
 * {@code complete()} method.
 *
 * If an error occurres while processing any Observable value it calls the Observers
 * {@code error(err)} method.
 */
export class Observable {

  /**
   * @param subscribe {observableInput}
   */
  constructor(subscribe) {
    this._subscribe = subscribe;
    this._subscriber = null;
  }

  /**
   * Subscribes a provided Observable to observe new values.
   *
   * @param next {nextCallback}
   * @param error? {errorCallback}
   * @param complete? {completeCallback}
   * @return {Subscription}
   */
  subscribe({next, error, complete}) {
    if (this._subscriber) {
      throw new Error("This observable already has a subscriber.");
    }

    this._subscriber = Subscriber.fromObservable(next, error, complete);

    try {
      this._subscribe({
        next: data => this._subscriber.next(data),
        error: err => this._subscriber.error(err),
        complete: () => this._subscriber.complete(),
      });
    } catch (err) {
      this._subscriber.error(err);
    }

    return new Subscription(() => {
      this._subscriber.unsubscribe();
    });
  }
}
