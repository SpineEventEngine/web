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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

@DisplayName("WaitingRepetitionsPolicy should")
class WaitingRepetitionsPolicyTest {

    private static final int OVERHEAD_MILLIS = 500;

    @Test
    @DisplayName("allow no wait between attempts")
    void noWait() {
        int repetitions = 5;
        WaitingRepetitionsPolicy policy = WaitingRepetitionsPolicy.noWait(repetitions);
        assertTimeout(Duration.ofMillis(OVERHEAD_MILLIS), () -> runAttempts(policy, repetitions));
    }

    @Test
    @DisplayName("allow 1 second wait between attempts")
    void oneSecond() {
        int repetitions = 5;
        WaitingRepetitionsPolicy policy = WaitingRepetitionsPolicy.oneSecondWait(repetitions);
        assertTimeout(Duration.ofSeconds(repetitions - 1)
                              .plusMillis(OVERHEAD_MILLIS),
                      () -> runAttempts(policy, repetitions));
    }

    @Test
    @DisplayName("allow constant time wait between attempts")
    void constSeconds() {
        int repetitions = 5;
        Duration waitTime = Duration.ofSeconds(2);
        WaitingRepetitionsPolicy policy =
                WaitingRepetitionsPolicy.constantWait(repetitions, waitTime);
        assertTimeout(waitTime.multipliedBy(repetitions - 1)
                              .plusMillis(OVERHEAD_MILLIS),
                      () -> runAttempts(policy, repetitions));
    }

    @Test
    @DisplayName("allow exponential time wait between attempts")
    void exponential() {
        int repetitions = 5;
        Duration waitTime = Duration.ofSeconds(2);
        int multiplier = 2;
        WaitingRepetitionsPolicy policy =
                WaitingRepetitionsPolicy.exponentialWait(repetitions, waitTime, multiplier);
        Duration total = Duration.ofSeconds(2 + 4 + 8 + 16)
                                 .plusMillis(OVERHEAD_MILLIS);
        assertTimeout(total, () -> runAttempts(policy, repetitions));
    }

    @Test
    @DisplayName("rethrow first exception")
    void throwFirst() {
        WaitingRepetitionsPolicy policy = WaitingRepetitionsPolicy.noWait(3);
        AtomicInteger counter = new AtomicInteger(0);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> policy.callAndRetry(() -> {
                    throw new IllegalStateException(String.valueOf(counter.incrementAndGet()));
                }));
        assertThat(exception).hasMessageThat().isEqualTo("1");
        assertThat(exception.getSuppressed()).hasLength(2);
    }

    private static void runAttempts(WaitingRepetitionsPolicy policy, int repetitionCount) {
        AtomicInteger attemptCount = new AtomicInteger(0);
        policy.runAndRetry(() -> {
            int attemptNumber = attemptCount.incrementAndGet();
            if (attemptNumber != repetitionCount) {
                throw new IllegalStateException(Integer.toString(attemptNumber));
            }
        });
    }
}
