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

import com.google.common.truth.IterableSubject;
import io.spine.logging.Logging;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.event.EventRecodingLogger;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import static com.google.common.truth.Truth.assertThat;
import static org.slf4j.event.Level.ERROR;

@DisplayName("Completion should")
class CompletionTest extends UtilityClassTest<Completion> {

    private Queue<SubstituteLoggingEvent> log;

    CompletionTest() {
        super(Completion.class);
    }

    @BeforeEach
    void setUp() {
        Logger completionLogger = Logging.get(Completion.class);
        SubstituteLogger substituteLogger = (SubstituteLogger) completionLogger;
        log = new ArrayDeque<>();
        substituteLogger.setDelegate(new EventRecodingLogger(substituteLogger, log));
    }

    @Test
    @DisplayName("ignore stages which complete successfully")
    void ignoreSuccessfulStages() {
        CompletableFuture<Number> successfulStage = new CompletableFuture<>();
        successfulStage.complete(42);
        Completion.dispose(successfulStage);
        assertThat(log).isEmpty();
    }

    @Test
    @DisplayName("log stage exceptions")
    void logExceptions() {
        CompletableFuture<Number> failedStage = new CompletableFuture<>();
        failedStage.completeExceptionally(new UnicornException());
        Completion.dispose(failedStage);

        IterableSubject assertLog = assertThat(log);
        assertLog.isNotEmpty();
        assertLog.hasSize(1);

        SubstituteLoggingEvent loggingEvent = log.poll();
        assertThat(loggingEvent.getLevel()).isEqualTo(ERROR);
        assertThat(loggingEvent.getThrowable())
                .isInstanceOf(UnicornException.class);
    }

    private static final class UnicornException extends Exception {
        private static final long serialVersionUID = 0L;
    }
}
