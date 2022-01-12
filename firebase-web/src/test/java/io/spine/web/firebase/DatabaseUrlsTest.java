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

import io.spine.net.Urls;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

@DisplayName("`DatabaseUrls` should")
class DatabaseUrlsTest extends UtilityClassTest<DatabaseUrls> {

    DatabaseUrlsTest() {
        super(DatabaseUrls.class);
    }

    @Test
    @DisplayName("be created from a remote RDB URL")
    void acceptRemoteRdb() {
        var dbUrl = "https://spine-dev.firebaseio.com";
        var url = DatabaseUrls.from(dbUrl);
        assertThat(url.getUrl())
                .isEqualTo(Urls.create(dbUrl));
        assertThat(url.getNamespace())
                .isEmpty();
    }

    @Test
    @DisplayName("be created from a local emulator URL")
    void acceptLocalEmulator() {
        var dbUrl = "http://localhost:5000?ns=spine-dev";
        var url = DatabaseUrls.from(dbUrl);
        assertThat(url.getUrl())
                .isEqualTo(Urls.create("http://localhost:5000"));
        assertThat(url.getNamespace())
                .isEqualTo("spine-dev");
    }
}
