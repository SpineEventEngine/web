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

package io.spine.web.future;

import io.spine.logging.Logging;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import java.util.concurrent.CompletionStage;

/**
 * A set of utilities for working with dignified ending for a {@code CompletionStage}.
 */
public final class Completion {

    private static final Logger log = Logging.get(Completion.class);

    /**
     * Prevents the utility class instantiation.
     */
    private Completion() {
    }

    /**
     * Logs the exception of the given stage if any.
     *
     * <p>Does nothing if the stage completes successfully.
     *
     * <p>Note that the method does not block the thread but logs the exception when the given stage
     * is complete.
     *
     * @param completionStage
     *         stage which may complete exceptionally
     */
    public static void dispose(CompletionStage<?> completionStage) {
        completionStage.whenComplete((result, exception) -> logException(exception));
    }

    private static void logException(@Nullable Throwable exception) {
        if (exception != null) {
            log.error("Failed to complete task.", exception);
        }
    }
}
