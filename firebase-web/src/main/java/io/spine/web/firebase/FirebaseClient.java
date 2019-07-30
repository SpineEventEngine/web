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

import java.util.Optional;

/**
 * A client which operates on values in the Firebase database.
 *
 * <p>The implementations are meant to work in "one client per database" format.
 */
public interface FirebaseClient {

    /**
     * Retrieves the value of the specified Firebase database node.
     *
     * <p>The {@code null} value (i.e. the node is not present in the database) is returned as
     * {@link java.util.Optional#empty()}.
     *
     * @param nodePath
     *         the path to the requested node in the database
     * @return the node value or empty {@code Optional} if the node is not present in the database
     */
    Optional<NodeValue> get(NodePath nodePath);

    /**
     * Writes the specified value to the Firebase database node.
     *
     * <p>If the node exists, the value is overridden.
     *
     * @param nodePath
     *         the path to the node in the Firebase database
     * @param value
     *         the value to write
     */
    void create(NodePath nodePath, NodeValue value);

    /**
     * Merges the specified value to the Firebase database node.
     *
     * <p>If the node doesn't exist, it is created with the given value.
     *
     * <p>If the node exists, the value entries are added to the node children overwriting common
     * ones if present.
     *
     * @param nodePath
     *         the path to the node in the Firebase database
     * @param value
     *         the value to merge
     */
    void update(NodePath nodePath, NodeValue value);

    void delete(NodePath path);
}
