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

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.function.Supplier;

/**
 * Strategy of retrying a request to Firebase.
 */
public interface RetryPolicy {

    /**
     * Calls the given routine and obtains its result.
     *
     * <p>If all the attempts fail (i.e. throw an exception), the thrown by the first attempt is
     * rethrown. All the other exceptions are added to it as
     * {@linkplain Exception#getSuppressed() suppressed} throwables.
     *
     * @param routine the routine to call and possibly retry
     * @param <T> the result type
     * @return the routine result
     */
    @CanIgnoreReturnValue
    <T> T callAndRetry(Supplier<T> routine);

    /**
     * Calls the given routine and obtains its result.
     *
     * <p>If all the attempts fail (i.e. throw an exception), the thrown by the first attempt is
     * rethrown. All the other exceptions are added to it as
     * {@linkplain Exception#getSuppressed() suppressed} throwables.
     *
     * @param routine the routine to call and possibly retry
     */
    @SuppressWarnings("ReturnOfNull") // OK since no result is expected.
    default void runAndRetry(Runnable routine) {
        this.callAndRetry(() -> {
            routine.run();
            return null;
        });
    }
}
