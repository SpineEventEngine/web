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
package io.spine.web.test.given.rest;

import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Back-off policy when executing an operation that could encounter transient failures.
 *
 * <p>On rare occasions, a request function might exit prematurely due to an internal error, and by default,
 * the function might or might not be automatically retried.
 *
 * <p>This class provides a possibility to execute {@code Runnable} with the default retry functionality. In case of
 * any exception occurs during {@code runnable.run()}, the default implementation will retry operation up to 2 times.
 *
 * <p>The backoff period for each retry attempt is multiplied by 2.
 *
 * @author Dmitry Kashcheiev
 */
public final class RestBackOffRunner {

    private static final int DEFAULT_NUMBER_OF_TRIES = 3;

    /**
     * Provides instance of {@link RestBackOffRunner}.
     *
     * @return instance of {@link RestBackOffRunner}
     */
    public static RestBackOffRunner create() {
        return new RestBackOffRunner();
    }

    public final void run(Runnable runnable) {
        run(runnable, DEFAULT_NUMBER_OF_TRIES);
    }

    /**
     * Execute provided runnable with the specified amount of attempts.
     *
     * @param runnable   the runnable to invoke with the retries
     * @param retryCount the maximum amount of attempts
     */
    public final void run(Runnable runnable, int retryCount) {
        checkArgument(retryCount > 0, "Backoff runner Exception, retryCount was set to 0");
        RuntimeException lastException = null;
        for (int i = 0; i < retryCount; i++) {
            try {
                runnable.run();
                return;
            } catch (RuntimeException e) {
                lastException = new RuntimeException(e);
                long sleepSec = Math.round(StrictMath.pow(2, i));
                Uninterruptibles.sleepUninterruptibly(sleepSec, TimeUnit.SECONDS);
            }
        }
        throw new RestServiceException(lastException);
    }
}
