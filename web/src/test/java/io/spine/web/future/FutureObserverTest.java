/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.truth.StringSubject;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`FutureObserver` should")
class FutureObserverTest {

    @Test
    @DisplayName("instantiate self")
    void testCreateDefault() {
        FutureObserver<String> observer = FutureObserver.create();
        String value = "hello";
        observer.onNext(value);
        assertResult(observer).isEqualTo(value);
        observer.onCompleted();
        assertResult(observer).isEqualTo(value);
    }

    @Test
    @DisplayName("complete with null by default")
    void testDefaultNull() {
        FutureObserver<String> observer = FutureObserver.create();
        observer.onCompleted();
        assertResult(observer).isNull();
    }

    @Test
    @DisplayName("complete with given default value")
    void testDefaultValue() {
        String defaultValue = "Aquaman";
        FutureObserver<String> observer = FutureObserver.withDefault(defaultValue);
        observer.onCompleted();
        assertResult(observer).isEqualTo(defaultValue);
    }

    @Test
    @DisplayName("fail to override value")
    void testFailToOverride() {
        FutureObserver<String> observer = FutureObserver.create();
        observer.onNext("first");
        assertThrows(IllegalStateException.class, () -> observer.onNext("second"));
    }

    @Test
    @DisplayName("override value with error if onError() called")
    void testOverrideWithError() {
        FutureObserver<String> observer = FutureObserver.create();
        String value = "Titanic";
        observer.onNext(value);
        assertEquals(value, observer.toFuture()
                                    .join());
        observer.onError(new IcebergCollisionException());
        Throwable thrown = assertThrows(
                CompletionException.class,
                () -> observerResult(observer)
        );
        Throwable rootCause = getRootCause(thrown);
        assertThat(rootCause).isInstanceOf(IcebergCollisionException.class);
    }

    private static StringSubject assertResult(FutureObserver<String> observer) {
        return assertThat(observerResult(observer));
    }

    @CanIgnoreReturnValue
    private static <T> T observerResult(FutureObserver<T> observer) {
        return observer
                .toFuture()
                .join();
    }

    /**
     * An exception thrown on an event on colliding with an iceberg.
     *
     * <p>This exception type exists in order to uniquely flag an error origin.
     */
    private static final class IcebergCollisionException extends Exception {

        private static final long serialVersionUID = 0L;
    }
}
