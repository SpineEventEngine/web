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

package io.spine.web.firebase.subscription;

import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.web.firebase.NodePaths;
import io.spine.web.firebase.client.NodePath;
import io.spine.web.subscription.result.SubscribeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.StringWriter;

import static io.spine.json.Json.toCompactJson;
import static io.spine.web.firebase.given.FirebaseResultTestEnv.mockWriter;
import static io.spine.web.firebase.given.FirebaseSubscribeResultTestEnv.newSubscription;
import static io.spine.web.firebase.given.FirebaseSubscribeResultTestEnv.newTarget;
import static io.spine.web.firebase.given.FirebaseSubscribeResultTestEnv.topicFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("FirebaseSubscribeResult should")
class FirebaseSubscribeResultTest {

    @Test
    @DisplayName("write DB path to servlet response")
    void testWritePath() throws IOException {
        ServletResponse response = mock(ServletResponse.class);
        StringWriter writer = mockWriter(response);

        TopicFactory topicFactory = topicFactory();
        Topic topic = topicFactory.forTarget(newTarget("test-type"));
        NodePath path = NodePaths.of("test-database-path");
        Subscription subscription = newSubscription(topic, path.getValue());
        SubscribeResult result = new FirebaseSubscribeResult(subscription);
        result.writeTo(response);
        verify(response).getWriter();

        String expected = toCompactJson(subscription);
        assertEquals(expected, writer.toString());
    }
}
