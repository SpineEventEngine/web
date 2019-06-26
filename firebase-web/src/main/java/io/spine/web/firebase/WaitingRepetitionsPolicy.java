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

import java.time.Duration;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.time.Duration.ZERO;

/**
 * A {@link RetryPolicy} which allows a given number of attempts separated by some wait time.
 */
public final class WaitingRepetitionsPolicy implements RetryPolicy {

    private static final Duration ONE_SECOND = Duration.ofSeconds(1);
    private static final int CONSTANT_MULTIPLIER = 1;

    private final int maxRepetitions;
    private final Duration seedWait;
    private final int multiplier;

    /**
     * Creates a new instance of {@code WaitingRepetitionsPolicy} which does not wait between
     * attempts.
     *
     * @param maxRepetitions
     *         the attempt count after which the request is considered failed
     */
    public static WaitingRepetitionsPolicy noWait(int maxRepetitions) {
        return incrementingWait(maxRepetitions, ZERO, CONSTANT_MULTIPLIER);
    }

    /**
     * Creates a new instance of {@code WaitingRepetitionsPolicy} which waits for 1 second between
     * attempts.
     *
     * @param maxRepetitions
     *         the attempt count after which the request is considered failed
     */
    public static WaitingRepetitionsPolicy oneSecondWait(int maxRepetitions) {
        return constantWait(maxRepetitions, ONE_SECOND);
    }

    /**
     * Creates a new instance of {@code WaitingRepetitionsPolicy} which waits for a given constant
     * time between attempts.
     *
     * @param maxRepetitions
     *         the attempt count after which the request is considered failed
     * @param waitTime
     *         the time to wait between attempts
     */
    public static WaitingRepetitionsPolicy constantWait(int maxRepetitions, Duration waitTime) {
        return incrementingWait(maxRepetitions, waitTime, CONSTANT_MULTIPLIER);
    }

    /**
     * Creates a new instance of {@code WaitingRepetitionsPolicy} which waits for a greater time
     * period between attempts.
     *
     * <p>If the given {@code seedWait} is {@code 2} seconds and the {@code multiplier} is equal to
     * {@code 3}, the resulting policy is to wait {@code 2} seconds after the first attempt,
     * {@code 6} seconds after the second attempt, {@code 18} seconds after the third attempt, and
     * so on.
     *
     * <p>Use this policy when integrating with API which may cache own responses.
     *
     * @param maxRepetitions
     *         the attempt count after which the request is considered failed
     * @param seedWait
     *         the time to wait after the first attempt
     * @param multiplier
     *         the number to multiply the last wait time by in order to get the next wait time
     */
    public static WaitingRepetitionsPolicy
    incrementingWait(int maxRepetitions, Duration seedWait, int multiplier) {
        checkArgument(maxRepetitions > 0, "Repetition count must be positive.");
        checkNotNull(seedWait);
        checkArgument(!seedWait.isNegative(), "Wait time must not be negative.");
        checkArgument(multiplier > 0, "Wait time multiplier must be positive.");
        return new WaitingRepetitionsPolicy(maxRepetitions, seedWait, multiplier);
    }

    private WaitingRepetitionsPolicy(int maxRepetitions, Duration seedWait, int multiplier) {
        this.maxRepetitions = maxRepetitions;
        this.seedWait = checkNotNull(seedWait);
        this.multiplier = multiplier;
    }

    @CanIgnoreReturnValue
    @Override
    public <T> T callAndRetry(Supplier<T> routine) {
        RuntimeException firstException = null;
        Duration backOffWait = seedWait;
        for (int i = 0; i < maxRepetitions; i++) {
            try {
                return routine.get();
            } catch (RuntimeException e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
                if (i != maxRepetitions - 1) {
                    sleepUninterruptibly(backOffWait);
                    backOffWait = backOffWait.multipliedBy(multiplier);
                }
            }
        }
        throw checkNotNull(firstException);
    }
}
