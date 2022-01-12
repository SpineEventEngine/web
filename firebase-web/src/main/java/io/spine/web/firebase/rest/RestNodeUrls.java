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

package io.spine.web.firebase.rest;

import com.google.api.client.http.GenericUrl;
import io.spine.net.Url;
import io.spine.net.Urls;
import io.spine.web.firebase.DatabaseUrl;
import io.spine.web.firebase.NodePath;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

/**
 * A factory creating {@linkplain RestNodeUrl REST Node URLs} of the Firebase database at the
 * specified {@linkplain DatabaseUrl url}.
 */
final class RestNodeUrls {

    private final DatabaseUrl database;

    /**
     * Creates a new factory for the specified {@code database}.
     */
    RestNodeUrls(DatabaseUrl database) {
        this.database = checkNotNull(database);
    }

    /**
     * Creates a new {@link RestNodeUrl} for a node at the specified {@link NodePath path}.
     */
    RestNodeUrl with(NodePath path) {
        checkNotNull(path);
        var url = withinDatabase(path);
        var node = RestNodeUrl.newBuilder()
                .setUrl(url)
                .vBuild();
        return node;
    }

    private Url withinDatabase(NodePath path) {
        var dbUrl = database.getUrl();
        String result;
        if (isNullOrEmpty(database.getNamespace())) {
            result = format("%s/%s.json", dbUrl.getSpec(), path.getValue());
        } else {
            result = format(
                    "%s/%s.json?ns=%s", dbUrl.getSpec(), path.getValue(), database.getNamespace()
            );
        }
        return Urls.create(result);
    }

    /**
     * Converts supplied {@code node} into a {@code GenericUrl}.
     */
    static GenericUrl asGenericUrl(RestNodeUrl node) {
        checkNotNull(node);
        var url = new GenericUrl(Urls.toString(node.getUrl()));
        return url;
    }
}
