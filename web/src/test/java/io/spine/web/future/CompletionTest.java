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

package io.spine.web.future;

import io.spine.logging.Logging;
import io.spine.testing.UtilityClassTest;
import io.spine.testing.logging.LogRecordSubject;
import io.spine.testing.logging.SimpleLoggingTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

@DisplayName("Completion should")
class CompletionTest extends UtilityClassTest<Completion> {

    CompletionTest() {
        super(Completion.class);
    }

    @Nested
    class LogOutputTest extends SimpleLoggingTest {

        LogOutputTest() {
            super(Completion.class, Logging.errorLevel());
        }

        @Test
        @DisplayName("ignore stages which complete successfully")
        void ignoreSuccessfulStages() {
            CompletableFuture<Number> successfulStage = new CompletableFuture<>();
            successfulStage.complete(42);
            Completion.dispose(successfulStage);

            assertLog().isEmpty();
        }

        @Test
        @DisplayName("log stage exceptions")
        void logExceptions() {
            CompletableFuture<Number> failedStage = new CompletableFuture<>();
            failedStage.completeExceptionally(new UnicornException());
            Completion.dispose(failedStage);

            LogRecordSubject assertLogRecord = assertLog().record();

            assertLogRecord.hasLevelThat()
                           .isEqualTo(Logging.errorLevel());
            assertLogRecord.hasThrowableThat()
                           .isInstanceOf(UnicornException.class);
        }
    }

    private static final class UnicornException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
