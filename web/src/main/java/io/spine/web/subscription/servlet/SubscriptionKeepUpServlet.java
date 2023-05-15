/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.client.Subscription;
import io.spine.web.MessageServlet;
import io.spine.web.subscription.SubscriptionBridge;

/**
 * An abstract servlet for a client request to keep up an existing {@link Subscription}.
 *
 * <p>This servlet parses the client requests and passes it to the {@link SubscriptionBridge}
 * to process. After, a processing result is written to the servlet response.
 *
 * @param <T>
 *         type of the response message
 */
@SuppressWarnings("serial") // Java serialization is not supported.
public abstract class SubscriptionKeepUpServlet<T extends Message>
        extends MessageServlet<Subscription, T> {

    private final SubscriptionBridge<?, T, ?> bridge;

    /**
     * Creates a new instance of {@code SubscriptionKeepUpServlet} with the given 
     * {@link SubscriptionBridge}.
     *
     * @param bridge
     *         the subscription bridge to be used to keep up subscriptions
     */
    protected SubscriptionKeepUpServlet(SubscriptionBridge<?, T, ?> bridge) {
        super();
        this.bridge = bridge;
    }

    @Override
    protected T handle(Subscription request) {
        return bridge.keepUp(request);
    }
}
