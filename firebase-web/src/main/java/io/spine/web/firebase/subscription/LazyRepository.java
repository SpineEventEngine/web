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

package io.spine.web.firebase.subscription;

import com.google.common.base.Suppliers;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A lazy-loading {@link SubscriptionRepository}.
 */
final class LazyRepository implements Supplier<SubscriptionRepository> {

    private final Supplier<SubscriptionRepository> delegate;

    private LazyRepository(Supplier<SubscriptionRepository> delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a new instance of {@code LazyRepository}.
     *
     * @param delegate a supplier which produces the resulting repository
     */
    static LazyRepository lazy(Supplier<SubscriptionRepository> delegate) {
        checkNotNull(delegate);
        Supplier<SubscriptionRepository> memoized = Suppliers.memoize(delegate::get);
        return new LazyRepository(memoized);
    }

    /**
     * Obtains the repository.
     *
     * <p>When called for the first time, invokes the given supplier and memoizes its result.
     * When called again, obtains the same instance as the first time.
     */
    @Override
    public SubscriptionRepository get() {
        return delegate.get();
    }
}
