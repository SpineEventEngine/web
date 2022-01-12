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

package io.spine.web.firebase.subscription;

import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A subscription record that gets stored into a Firebase database.
 *
 * <p>Supports both an initial store and consequent updates of the stored data.
 */
final class SubscriptionRecord {

    private final NodePath path;
    private final UpdatePayload updatePayload;

    SubscriptionRecord(NodePath path, UpdatePayload payload) {
        this.path = checkNotNull(path);
        this.updatePayload = checkNotNull(payload);
    }

    /**
     * Stores the data to the Firebase updating only the data that has changed.
     */
    void store(FirebaseClient firebaseClient) {
        var nodeValue = updatePayload.asNodeValue();
        firebaseClient.update(path, nodeValue);
    }
}
