/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.web.firebase;

import com.google.firebase.database.ChildEventListener;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link FirebaseClient} which executes write operations asynchronously.
 *
 * <p>Read operations are considered less frequent and less costly, thus are executed synchronously.
 */
public final class AsyncClient implements FirebaseClient {

    private final FirebaseClient delegate;
    private final Executor executor;

    /**
     * Creates a new async client with the given delegate and the given executor.
     *
     * <p>It is a responsibility of the user to shut down the executor gracefully.
     *
     * @param delegate the firebase client which performs the requests
     * @param executor the {@link Executor} which executes the write requests
     */
    public AsyncClient(FirebaseClient delegate, Executor executor) {
        this.delegate = checkNotNull(delegate);
        this.executor = checkNotNull(executor);
    }

    /**
     * Creates a new async client with the given delegate.
     *
     * <p>The resulting client uses the {@link ForkJoinPool#commonPool()} to execute the write
     * operations. Note that this is the same {@code Executor} which is used by default in the Java
     * concurrency API, such as {@link java.util.concurrent.CompletableFuture} and the conventional
     * implementations of {@link java.util.stream.Stream}.
     *
     * @param delegate the firebase client which performs the requests
     * @see #AsyncClient(FirebaseClient, Executor)
     */
    public AsyncClient(FirebaseClient delegate) {
        this(delegate, ForkJoinPool.commonPool());
    }

    @Override
    public Optional<NodeValue> fetchNode(NodePath nodePath) {
        return delegate.fetchNode(nodePath);
    }

    @Override
    public void subscribeTo(NodePath nodePath, ChildEventListener listener) {
        delegate.subscribeTo(nodePath, listener);
    }

    @Override
    public void create(NodePath nodePath, NodeValue value) {
        executor.execute(() -> delegate.create(nodePath, value));
    }

    @Override
    public void update(NodePath nodePath, NodeValue value) {
        executor.execute(() -> delegate.update(nodePath, value));
    }

    @Override
    public void delete(NodePath nodePath) {
        executor.execute(() -> delegate.delete(nodePath));
    }
}
