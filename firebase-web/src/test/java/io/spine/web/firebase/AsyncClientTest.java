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

import com.google.common.util.concurrent.Uninterruptibles;
import io.spine.web.firebase.given.TestFirebaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

@DisplayName("Async Firebase client should")
class AsyncClientTest {

    private TestFirebaseClient delegate;
    private NodePath path;
    private ExecutorService executor;
    private Duration latency;

    @BeforeEach
    void setUp() {
        latency = ofSeconds(2);
        delegate = TestFirebaseClient.withSimulatedLatency(latency);
        path = NodePaths.of("some/kind/of/path");
        executor = newSingleThreadExecutor();
    }

    @SuppressWarnings("CheckReturnValue")
    @Test
    @DisplayName("perform read operations directly")
    void readDirectly() {
        AsyncClient asyncClient = new AsyncClient(delegate);
        asyncClient.fetchNode(path);
        assertThat(delegate.reads()).contains(path);
    }

    @Test
    @DisplayName("perform write operations with the given executor")
    void executeWrites() {
        AsyncClient asyncClient = new AsyncClient(delegate, executor);
        checkAsync(asyncClient);
    }

    @Test
    @DisplayName("perform write operations with asynchronously by default")
    void executeWritesWithForkJoinPool() {
        AsyncClient asyncClient = new AsyncClient(delegate);
        checkAsync(asyncClient);
    }

    @Test
    @DisplayName("allow to use the direct executor")
    void allowDirectExecutor() {
        AsyncClient asyncClient = new AsyncClient(delegate, directExecutor());
        asyncClient.create(path, NodeValue.empty());
        assertThat(delegate.writes()).contains(path);
    }

    private void checkAsync(AsyncClient asyncClient) {
        asyncClient.update(path, NodeValue.empty());
        assertThat(delegate.writes()).doesNotContain(path);
        Duration surefireTime = latency.plusSeconds(1);
        Uninterruptibles.sleepUninterruptibly(surefireTime);
        assertThat(delegate.writes()).contains(path);
    }
}
