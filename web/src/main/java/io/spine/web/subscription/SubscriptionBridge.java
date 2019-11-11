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

package io.spine.web.subscription;

import com.google.protobuf.Message;
import io.spine.client.Subscription;
import io.spine.client.Topic;

/**
 * A bridge for requests to a subscription {@link io.spine.server.SubscriptionService}.
 *
 * <p>Defines an interface for {@link #subscribe(Topic) subscribing} to a {@link Topic},
 * {@link #keepUp(Subscription) keeping up} an existing {@link Subscription}
 * and {@link #cancel(Subscription) canceling} an existing {@code Subscription}.
 *
 * @param <S>
 *         response type for {@link #subscribe(Topic)} requests
 * @param <K>
 *         response type for {@link #keepUp(Subscription)} requests
 * @param <C>
 *         response type for {@link #cancel(Subscription)} requests
 */
public interface SubscriptionBridge<S extends Message, K extends Message, C extends Message> {

    /**
     * Creates a new {@link Subscription} to a provided topic supplying this subscription to the
     * client as a result.
     *
     * @param topic
     *         a topic to subscribe the client to
     * @return a message describing the created subscription
     */
    S subscribe(Topic topic);

    /**
     * Keep up the subscription, prohibiting it from closing from the server-side.
     *
     * <p>This operation is performed because subscription can only live some finite amount of time.
     * Server cancels the subscription at some point, because maintaining the subscription requires
     * resources and the client cannot be trusted to cancel every subscription it creates.
     *
     * @param subscription
     *         a subscription that should stay open
     * @return the keep-up response
     */
    K keepUp(Subscription subscription);

    /**
     * Cancel the existing subscription, which stopping sending new data updates to the client.
     *
     * @param subscription
     *         a subscription that should be stopped from receiving updates
     * @return the cancellation response
     */
    C cancel(Subscription subscription);
}
