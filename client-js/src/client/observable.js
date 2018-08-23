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
 * @callback consumerCallback
 * @param {V} param
 * @template <V>
 */

/**
 * @callback voidCallback
 */

/**
 * @callback observableFunction
 * @param {Observer<V>} observer
 * @returns {voidCallback}
 * @template <V, B>
 */

/**
 * An abstract Observer class.
 *
 * It is passed new values from the observable to its `#next(value)` method,
 * errors to `#error(err)` method and a notification about Observable completion to its
 * `complete()` method.
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
 * @template <V> a type of the value observed by this Observer
 * @template <E> a type of the accepted error 
 */
class Observer {
  // noinspection JSMethodCanBeStatic
  /**
   * Invoked by the observable when it retrieves a new value.
   *
   * @param {V} value next value observed in a `Observable`
   */
  next(value) {
    throw new Error('Unimplemented by an abstract Observer');
  }

  // noinspection JSMethodCanBeStatic
  /**
   * Invoked by the observable an error occurs while processing its values.
   *
   * @param {E} err an error which occurred observing the value
   */
  error(err) {
    throw new Error('Unimplemented by an abstract Observer');
  }

  // noinspection JSMethodCanBeStatic
  /**
   * Invoked by the observable when its complete.
   */
  complete() {
    throw new Error('Unimplemented by an abstract Observer');
  }
}

/**
 * A subscription returned by the observable allowing to unsubscribe the Observer from receiving
 * new values.
 */
export class Subscription {

  constructor(unsubscribe) {
    this._unsubscribe = unsubscribe;
    this._tearDownCallbacks = [];
    this.closed = false;
  }

  /**
   * Unsubscribes the subscription target stopping it from receiving new values.
   */
  unsubscribe() {
    if (this.closed) {
      throw "Tried to unsubscribe from closed subscription";
    }
    this._unsubscribe();
    this._tearDownCallbacks.forEach(callback => callback());
    this.closed = true;
  }

  /**
   * Adds tear down logic to this Subscription.
   * @param {!voidCallback} tearDown a callback invoked before unsubscribing.
   */
  add(tearDown) {
    this._tearDownCallbacks.push(tearDown);
  }
}

/**
 * An observable subscriber sending off received values, errors and complete notifications to the
 * observer.
 *
 * @template <N> a type of the value observed by this Observer
 * @template <E> a type of the accepted error
 */
class Subscriber {

  /**
   * @param {Observer<N, E>} destination
   */
  constructor(destination) {
    this._destination = destination;
  }

  /**
   * Sends off the next observable value to the Observer, skipping it if the observable was
   * unsubscribed or the Observer was complete.
   *
   * @param {N} value a next value for the Observer
   */
  next(value) {
    if (!this.isStopped) {
      this._destination.next(value);
    }
  }

  /**
   * Sends off an error to the Observer, stopping it from receiving further values.
   *
   * @param {E} err an error to be passed to the Observer
   */
  error(err) {
    if (!this.isStopped) {
      this.isStopped = true;
      this._destination.error(err);
      this.unsubscribe();
    }
  }

  /**
   * Sends off a complete notification to the Observer, stopping it from receiving further values.
   */
  complete() {
    if (!this.isStopped) {
      this.isStopped = true;
      this._destination.complete();
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
   * `next` and `complete` use no-op as a default, while the `error` is logging
   * the values to the console.
   *
   * @param {!consumerCallback<N>} next
   * @param {?consumerCallback} error
   * @param {?voidCallback} complete
   * @return {Subscriber<N>}
   */
  static fromObservable(next, error, complete) {

    const observer = {
      next,
      error: error || this._consoleErrorHandler,
      complete: complete || this._noop
    };

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
   * A default error handler used by the `Observable`, logging the error to console.
   * @private
   */
  static _consoleErrorHandler(error) {
    console.error(error);
  }
}

/**
 * An implementation of Observable pattern.
 *
 * An Observable represents a set of values over some period of time.
 *
 * An Observable accepts a single subscriber, supplying each new observed value to its
 * `next(item)` method.
 *
 * When all of the possible values are observed an observable call Observers
 * `complete()` method.
 *
 * If an error occurs while processing any Observable value it calls the Observers
 * `error(err)` method.
 *
 * @template <N> a type of the next observed value
 * @template <E> a type of error that can occur in observer
 */
export class Observable {

  /**
   * @param {!observableFunction<N>} subscribe
   */
  constructor(subscribe) {
    this._subscribe = subscribe;
    this._subscriber = null;
  }

  /**
   * Subscribe the Observable to observe new values.
   *
   * @param {!consumerCallback<N>} next receives next observed value
   * @param {?consumerCallback<E>} error receives error which occured while observing values 
   *                                     stopping an observer from receiving any new values 
   * @param {?voidCallback} complete a callback invoked when observable values came to an end 
   * @return {Subscription}
   */
  subscribe({next, error, complete}) {
    if (this._subscriber) {
      throw new Error('This observable already has a subscriber.');
    }

    /** @type Subscriber<N> */
    this._subscriber = Subscriber.fromObservable(next, error, complete);

    let tearDown;
    try {
      tearDown = this._subscribe({
        next: data => this._subscriber.next(data),
        error: err => this._subscriber.error(err),
        complete: () => this._subscriber.complete(),
      });
    } catch (err) {
      this._subscriber.error(err);
    }

    const subscription = new Subscription(() => {
      this._subscriber.unsubscribe();
    });

    if (tearDown) {
      subscription.add(tearDown);
    }

    return subscription;
  }
}
