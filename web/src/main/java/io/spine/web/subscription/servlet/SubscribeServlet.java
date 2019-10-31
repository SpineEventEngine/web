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

package io.spine.web.subscription.servlet;

import com.google.protobuf.Message;
import io.spine.client.Topic;
import io.spine.web.MessageServlet;
import io.spine.web.subscription.SubscriptionBridge;
import io.spine.web.subscription.result.SubscribeResult;

/**
 * An abstract servlet for a client request to create a new  {@code Subscription} for the requested
 * {@link Topic Topic}.
 *
 * <p>This servlet parses the client requests and passes it to the {@link SubscriptionBridge}
 * to process. After, {@linkplain SubscribeResult the processing result} is written to
 * the servlet response.
 */
@SuppressWarnings("serial") // Java serialization is not supported.
public abstract class SubscribeServlet<T extends Message> extends MessageServlet<Topic, T> {

    private final SubscriptionBridge<T, ?, ?> bridge;

    /**
     * Creates a new instance of {@code SubscribeServlet} with the given
     * {@link SubscriptionBridge}.
     *
     * @param bridge
     *         the subscription bridge to be used to create subscriptions
     */
    protected SubscribeServlet(SubscriptionBridge<T, ?, ?> bridge) {
        super();
        this.bridge = bridge;
    }

    @Override
    protected T handle(Topic request) {
        return bridge.subscribe(request);
    }
}
