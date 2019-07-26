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
     * Writes the given value under the given path in the database.
     *
     * <p>Overrides any existing value at the given path.
     *
     * @param path
     *         the path to the node in the database
     * @param value
     *         the value to write into the database
     * @see #update(NodePath, NodeValue)
     */
    void create(NodePath path, NodeValue value);

    /**
     * Updates the value under the given database path with the given value.
     *
     * <p>If there is an existing value of the given node, the values are merged:
     * <ul>
     *     <li>common values are overridden;
     *     <li>non-common values are preserved.
     * </ul>
     *
     * @param path
     *         the path to the node in the database
     * @param value
     *         the value to merge into the database node
     * @see #create(NodePath, NodeValue)
     */
    void update(NodePath path, NodeValue value);
}
