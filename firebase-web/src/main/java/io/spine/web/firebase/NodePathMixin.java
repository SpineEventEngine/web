/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import io.spine.annotation.GeneratedMixin;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A generated mixin interface for the {@link NodePath} message type.
 */
@SuppressWarnings("ClassReferencesSubclass")
@GeneratedMixin
interface NodePathMixin extends NodePathOrBuilder {

    /**
     * Generates a new path by concatenating this path with the given {@code other} one.
     *
     * <p>For example, if this path if {@code root/child/sub-child} and the {@code other} is
     * {@code aaa/bbb}, the resulting path would be {@code root/child/sub-child/aaa/bbb}.
     *
     * @param other
     *         path to append
     * @return the concatenated path
     */
    default NodePath append(NodePath other) {
        checkNotNull(other);
        return append(other.getValue());
    }

    /**
     * Generates a new path by concatenating this path with the given string representing another
     * path.
     *
     * <p>For example, if this path if {@code root/child/sub-child} and the given raw string is
     * {@code "aaa/bbb"}, the resulting path would be {@code root/child/sub-child/aaa/bbb}.
     *
     * @param rawPath
     *         path to append
     * @return the concatenated path
     */
    default NodePath append(String rawPath) {
        checkNotNull(rawPath);
        checkArgument(!rawPath.isEmpty());
        return NodePaths.of(this.getValue(), rawPath);
    }
}
