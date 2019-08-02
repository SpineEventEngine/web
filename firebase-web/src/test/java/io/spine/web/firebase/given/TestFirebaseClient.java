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

package io.spine.web.firebase.given;

import com.google.common.collect.ImmutableList;
import com.google.firebase.database.ChildEventListener;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.google.appengine.repackaged.com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

public final class TestFirebaseClient implements FirebaseClient {

    private final List<NodePath> reads = newArrayList();
    private final List<NodePath> writes = newArrayList();

    private final Duration writeLatency;

    private TestFirebaseClient(Duration latency) {
        this.writeLatency = latency;
    }

    public static TestFirebaseClient withSimulatedLatency(Duration latency) {
        checkNotNull(latency);
        return new TestFirebaseClient(latency);
    }

    @Override
    public Optional<NodeValue> get(NodePath nodePath) {
        reads.add(nodePath);
        return Optional.empty();
    }

    @Override
    public void subscribeTo(NodePath path, ChildEventListener listener) {
        reads.add(path);
    }

    @Override
    public void create(NodePath nodePath, NodeValue value) {
        sleepUninterruptibly(writeLatency);
        writes.add(nodePath);
    }

    @Override
    public void update(NodePath nodePath, NodeValue value) {
        sleepUninterruptibly(writeLatency);
        writes.add(nodePath);
    }

    @Override
    public void delete(NodePath nodePath) {
        sleepUninterruptibly(writeLatency);
        writes.add(nodePath);
    }

    public ImmutableList<NodePath> reads() {
        return ImmutableList.copyOf(reads);
    }

    public ImmutableList<NodePath> writes() {
        return ImmutableList.copyOf(writes);
    }
}
