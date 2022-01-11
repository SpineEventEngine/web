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

package io.spine.web.query;

import com.google.common.collect.ImmutableSet;
import io.spine.protobuf.AnyPacker;
import io.spine.test.web.Task;
import io.spine.test.web.TaskId;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.web.given.TestQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`BlockingQueryBridge` should")
class BlockingQueryBridgeTest {

    private static final TestActorRequestFactory requests =
            new TestActorRequestFactory(BlockingQueryBridgeTest.class);

    @Test
    @DisplayName("obtain a `QueryResponse`")
    void obtainQueryResponse() {
        var id = TaskId.generate();
        var task = Task.newBuilder()
                .setId(id)
                .setTitle(BlockingQueryBridgeTest.class.getSimpleName())
                .build();
        var service = new TestQueryService(task);
        var query = requests.query()
                            .byIds(Task.class, ImmutableSet.of(id));
        var bridge = new BlockingQueryBridge(service);
        var response = bridge.send(query);
        assertThat(response.getMessageCount()).isEqualTo(1);
        var message = response.getMessage(0).getState();
        assertThat(AnyPacker.unpack(message))
                .isEqualTo(task);
    }
}
