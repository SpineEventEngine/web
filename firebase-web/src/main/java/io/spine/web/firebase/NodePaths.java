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

import com.google.common.base.Joiner;

import java.util.Collection;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

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

    public static NodePath of(String... pathElements) {
        checkNotNull(pathElements);
        checkArgument(pathElements.length > 0);
        String path = concatPath(pathElements);
        return of(path);
    }

    public static NodePath of(String path) {
        checkNotNull(path);
        return NodePath
                .newBuilder()
                .setValue(escaped(path))
                .vBuild();
    }

    private static String concatPath(String... elements) {
        Collection<String> pathElements = newArrayList();
        for (String element : elements) {
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
