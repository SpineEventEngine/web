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

package io.spine.web.firebase;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link FirebaseClient} which retries failed requests.
 *
 * <p>This implementation uses another instance of {@code FirebaseClient} to perform the requests.
 */
public final class RetryingClient implements FirebaseClient {

    private final FirebaseClient delegate;
    private final RetryPolicy retryPolicy;

    /**
     * Creates a new {@code RetryingClient}.
     *
     * @param delegate
     *         the delegate {@code FirebaseClient} which performs the request on behalf of this
     *         client
     * @param policy
     *         the request retry policy
     */
    public RetryingClient(FirebaseClient delegate, RetryPolicy policy) {
        this.delegate = checkNotNull(delegate);
        this.retryPolicy = checkNotNull(policy);
    }

    @Override
    public Optional<NodeValue> get(NodePath nodePath) {
        return retryPolicy.callAndRetry(() -> delegate.get(nodePath));
    }

    @Override
    public void merge(NodePath nodePath, NodeValue value) {
        retryPolicy.runAndRetry(() -> delegate.merge(nodePath, value));
    }
}
