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

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A static factory for {@link NodePath}.
 */
public final class NodePaths {

    private static final Pattern ILLEGAL_DATABASE_PATH_SYMBOL = Pattern.compile("[\\[\\].$#]");
    private static final String SUBSTITUTION_SYMBOL = "-";
    private static final char SEPARATOR = '/';
    private static final Joiner pathJoiner = Joiner.on(SEPARATOR);

    /** Prevents instantiation of this static factory. */
    private NodePaths() {
    }

    /**
     * Creates a {@link NodePath} by joining the given elements.
     *
     * <p>The elements are also checked for illegal path symbols. Such symbols
     * ({@literal [, ], ., $, #}) are replaced with the {@code -} (hyphen) symbol.
     *
     * @param pathElements
     *         the elements of a database path
     * @return the path
     */
    public static NodePath of(String... pathElements) {
        checkNotNull(pathElements);
        checkArgument(pathElements.length > 0);
        var path = concatPath(pathElements);
        return of(path);
    }

    /**
     * Creates a {@link NodePath} from the given string.
     *
     * <p>The path string is checked for illegal path symbols. Such symbols
     * ({@literal [, ], ., $, #}) are replaced with the {@code -} (hyphen) symbol.
     *
     * @param path
     *         the path string
     * @return the path
     */
    public static NodePath of(String path) {
        checkNotNull(path);
        return NodePath
                .newBuilder()
                .setValue(escaped(path))
                .vBuild();
    }

    private static String concatPath(String... elements) {
        var pathElements = new ArrayList<String>(elements.length);
        for (var element : elements) {
            if (!element.isEmpty()) {
                pathElements.add(element);
            }
        }
        String path = pathJoiner.join(pathElements);
        return path;
    }

    private static String escaped(String dirty) {
        return ILLEGAL_DATABASE_PATH_SYMBOL
                .matcher(dirty)
                .replaceAll(SUBSTITUTION_SYMBOL);
    }
}
