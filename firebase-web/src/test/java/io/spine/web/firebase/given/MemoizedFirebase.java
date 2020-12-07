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

package io.spine.web.firebase.given;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.database.ChildEventListener;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A Firebase client that memoizes read and write operations.
 *
 * <p>Supports having a custom write latency through {@linkplain #withSimulatedLatency(Duration)
 * setting} a particular write operations duration.
 */
public final class MemoizedFirebase implements FirebaseClient {

    private final Map<NodePath, NodeValue> writes = new HashMap<>();
    private final Collection<NodePath> reads = new ArrayList<>();

    private final Duration writeLatency;

    private MemoizedFirebase(Duration latency) {
        this.writeLatency = latency;
    }

    /**
     * Creates a new instance with zero latency.
     */
    public static MemoizedFirebase withNoLatency() {
        return withSimulatedLatency(Duration.ZERO);
    }

    /**
     * Creates a new instance with simulated {@code latency}.
     */
    public static MemoizedFirebase withSimulatedLatency(Duration latency) {
        checkNotNull(latency);
        return new MemoizedFirebase(latency);
    }

    @Override
    public Optional<NodeValue> fetchNode(NodePath nodePath) {
        reads.add(nodePath);
        return Optional.ofNullable(writes.get(nodePath));
    }

    @Override
    public void subscribeTo(NodePath nodePath, ChildEventListener listener) {
        reads.add(nodePath);
    }

    @Override
    public void create(NodePath nodePath, NodeValue value) {
        sleepUninterruptibly(writeLatency);
        writes.put(nodePath, value);
    }

    @Override
    public void update(NodePath nodePath, NodeValue value) {
        sleepUninterruptibly(writeLatency);
        writes.put(nodePath, value);
    }

    @Override
    public void delete(NodePath nodePath) {
        sleepUninterruptibly(writeLatency);
        writes.remove(nodePath);
    }

    /**
     * Returns a copy of all made read operations.
     */
    public ImmutableList<NodePath> reads() {
        return ImmutableList.copyOf(reads);
    }

    /**
     * Returns a copy of all made write operation.
     */
    public ImmutableMap<NodePath, NodeValue> writes() {
        return ImmutableMap.copyOf(writes);
    }

    /**
     * Returns a {@code NodeValue} written to a specific {@code path}.
     *
     * @throws IllegalStateException
     *         if no value is present for the path.
     */
    public NodeValue valueFor(NodePath path) {
        NodeValue result = writes.get(path);
        if (result == null) {
            throw newIllegalStateException(
                    "A value is expected to be present at path `%s`.", path.getValue()
            );
        }
        return result;
    }
}
