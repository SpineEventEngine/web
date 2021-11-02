/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.web.subscription;

import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.web.Cancel;
import io.spine.web.KeepUp;
import io.spine.web.Subscribe;
import io.spine.web.SubscriptionsCancelled;
import io.spine.web.SubscriptionsCreated;
import io.spine.web.SubscriptionsKeptUp;

/**
 * A bridge for requests to a subscription {@link io.spine.server.SubscriptionService}.
 *
 * <p>Defines an interface for {@link #subscribe subscribing} to a {@link Topic},
 * {@link #keepUp keeping up} an existing {@link Subscription}
 * and {@link #cancel canceling} an existing {@code Subscription}.
 */
public interface SubscriptionBridge {

    /**
     * Creates subscriptions for given topics for the provided lifetime.
     *
     * @param request
     *         the request to create subscriptions
     * @return detailed response addressing the success of each individual subscription
     */
    SubscriptionsCreated subscribe(Subscribe request);

    /**
     * Prolongs the lifetime of subscriptions.
     *
     * @param request
     *         the request to prolong the lifetime of certain subscriptions by
     *         a certain duration
     * @return detailed response addressing the success of each individual subscription keep-up
     */
    SubscriptionsKeptUp keepUp(KeepUp request);

    /**
     * Cancels given subscriptions.
     *
     * @param request
     *         the request to cancel subscriptions by their IDs
     * @return detailed response addressing the success of each individual cancellation
     */
    SubscriptionsCancelled cancel(Cancel request);
}
