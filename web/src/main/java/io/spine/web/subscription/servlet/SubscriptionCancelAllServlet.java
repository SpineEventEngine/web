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

package io.spine.web.subscription.servlet;

import io.spine.client.Subscription;
import io.spine.web.MessageServlet;
import io.spine.web.Responses;
import io.spine.web.Subscriptions;
import io.spine.web.subscription.SubscriptionBridge;

/**
 * An abstract servlet for a client request to cancel a batch of {@link Subscription}s.
 *
 * <p>This servlet parses the client requests and passes it to the {@link SubscriptionBridge}
 * to process. After, a processing result is written to the servlet response.
 */
@SuppressWarnings("serial") // Java serialization is not supported.
public abstract class SubscriptionCancelAllServlet
        extends MessageServlet<Subscriptions, Responses> {

    private final SubscriptionBridge<?, ?, ?> bridge;

    /**
     * Creates a new instance of {@code SubscriptionCancelAllServlet} with the given
     * {@link SubscriptionBridge}.
     *
     * @param bridge
     *         the subscription bridge to be used in to cancel subscriptions
     */
    protected SubscriptionCancelAllServlet(SubscriptionBridge<?, ?, ?> bridge) {
        super();
        this.bridge = bridge;
    }

    @Override
    protected Responses handle(Subscriptions request) {
        return bridge.cancelAll(request);
    }
}
