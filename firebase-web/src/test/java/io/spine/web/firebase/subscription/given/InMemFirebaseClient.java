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

package io.spine.web.firebase.subscription.given;

import com.google.firebase.database.ChildEventListener;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemFirebaseClient implements FirebaseClient {

    private final Map<NodePath, NodeValue> values = new HashMap<>();

    @Override
    public Optional<NodeValue> fetchNode(NodePath nodePath) {
        return Optional.ofNullable(values.get(nodePath));
    }

    @Override
    public void subscribeTo(NodePath nodePath, ChildEventListener listener) {
        // Method `subscribeTo` is not supported. OK for test purposes.
    }

    @Override
    public void create(NodePath nodePath, NodeValue value) {
        values.put(nodePath,value);
    }

    @Override
    public void update(NodePath nodePath, NodeValue value) {
        values.put(nodePath,value);
    }

    @Override
    public void delete(NodePath nodePath) {
        values.remove(nodePath);
    }
}
