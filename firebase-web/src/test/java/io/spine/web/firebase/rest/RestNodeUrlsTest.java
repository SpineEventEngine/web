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

package io.spine.web.firebase.rest;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import io.spine.net.Urls;
import io.spine.web.firebase.DatabaseUrl;
import io.spine.web.firebase.DatabaseUrls;
import io.spine.web.firebase.NodePaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

@DisplayName("`RestNodeUrls` should")
final class RestNodeUrlsTest {

    @Test
    @DisplayName("not accept `null`s")
    void notAcceptNulls() {
        NullPointerTester tester = new NullPointerTester();
        tester.testConstructors(RestNodeUrls.class, Visibility.PACKAGE);
        tester.testAllPublicInstanceMethods(new RestNodeUrls(DatabaseUrl.getDefaultInstance()));
    }

    @Test
    @DisplayName("create a `RestNodeUrl` for the specified `NodePath` and remote RDB")
    void createRemoteDbUrl() {
        String dbUrl = "https://spine-dev.firebaseio.com";
        RestNodeUrls factory = new RestNodeUrls(DatabaseUrls.from(dbUrl));
        String node = "subscriptions/111";
        RestNodeUrl result = factory.with(NodePaths.of(node));
        assertThat(result.getUrl())
                .isEqualTo(Urls.create(dbUrl + '/' + node + ".json"));
    }

    @Test
    @DisplayName("create a `RestNodeUrl` for the specified `NodePath` and local emulator")
    void createEmulatorUrl() {
        String dbUrl = "http://localhost:5000?ns=spine-dev";
        RestNodeUrls factory = new RestNodeUrls(DatabaseUrls.from(dbUrl));
        String node = "query/currentYear";
        RestNodeUrl result = factory.with(NodePaths.of(node));
        assertThat(result.getUrl())
                .isEqualTo(Urls.create("http://localhost:5000/" + node + ".json?ns=spine-dev"));
    }
}
