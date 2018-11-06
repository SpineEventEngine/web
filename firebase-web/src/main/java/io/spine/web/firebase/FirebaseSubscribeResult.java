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

package io.spine.web.firebase;

import io.spine.client.Subscription;
import io.spine.web.subscription.result.SubscribeResult;

import javax.servlet.ServletResponse;
import java.io.IOException;

import static io.spine.json.Json.toCompactJson;

/**
 * A result of a request to subscribe to some {@link io.spine.client.Topic Topic}
 * to be written to the {@link ServletResponse}.
 *
 * <p>The result is a JSON formatted {@link Subscription} message.
 */
class FirebaseSubscribeResult implements SubscribeResult {

    private final Subscription subscription;

    FirebaseSubscribeResult(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void writeTo(ServletResponse response) throws IOException {
        response.getWriter()
                .write(toCompactJson(subscription));
    }
}
