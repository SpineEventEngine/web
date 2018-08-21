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

package io.spine.web.subscription.servlet;

import io.spine.client.Subscription;
import io.spine.web.NonSerializableServlet;
import io.spine.web.parser.HttpMessages;
import io.spine.web.query.QueryBridge;
import io.spine.web.subscription.SubscriptionBridge;
import io.spine.web.subscription.result.SubscriptionCancelResult;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * An abstract servlet for a client request to cancel an existing {@link Subscription}.
 *
 * <p>This servlet parses the client requests and passes it to the {@link SubscriptionBridge}
 * to process. After, {@link SubscriptionCancelResult the processing result} is written to
 * the servlet response.
 *
 * @author Mykhailo Drachuk
 */
@SuppressWarnings("serial") // Java serialization is not supported.
public abstract class SubscriptionCancelServlet extends NonSerializableServlet {

    private final SubscriptionBridge bridge;

    /**
     * Creates a new instance of {@link SubscriptionCancelServlet} with the given {@link QueryBridge}.
     *
     * @param bridge the query bridge to be used in this query servlet
     */
    protected SubscriptionCancelServlet(SubscriptionBridge bridge) {
        super();
        this.bridge = bridge;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles the {@code POST} request through the {@link SubscriptionBridge}.
     */
    @OverridingMethodsMustInvokeSuper
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Optional<Subscription> optionalSubscription = HttpMessages.parse(req, Subscription.class);
        if (!optionalSubscription.isPresent()) {
            resp.sendError(SC_BAD_REQUEST);
        } else {
            Subscription subscription = optionalSubscription.get();
            SubscriptionCancelResult result = bridge.cancel(subscription);
            result.writeTo(resp);
        }
    }
}
