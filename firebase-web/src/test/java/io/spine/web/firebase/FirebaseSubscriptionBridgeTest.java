/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.client.grpc.QueryServiceGrpc.QueryServiceImplBase;
import io.spine.core.Response;
import io.spine.web.subscription.result.SubscribeResult;
import io.spine.web.subscription.result.SubscriptionCancelResult;
import io.spine.web.subscription.result.SubscriptionKeepUpResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.StringWriter;

import static io.spine.json.Json.fromJson;
import static io.spine.json.Json.toCompactJson;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.assertSubscriptionPointsToFirebase;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.mockWriter;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newBridge;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newResponse;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newSubscription;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newTarget;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.topicFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@DisplayName("FirebaseSubscriptionBridge should")
class FirebaseSubscriptionBridgeTest {

    private FirebaseSubscriptionBridge bridge;
    private TopicFactory topicFactory;

    @BeforeEach
    void setUp() {
        QueryServiceImplBase queryService = mock(QueryServiceImplBase.class);
        FirebaseClient firebaseClient = mock(FirebaseClient.class);
        bridge = newBridge(firebaseClient, queryService);
        topicFactory = topicFactory();
    }

    @Test
    @DisplayName("write OK response upon subscription keep up")
    void keepUpSubscription() throws IOException {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscription subscription = newSubscription(topic);

        SubscriptionKeepUpResult result = bridge.keepUp(subscription);

        ServletResponse response = mock(ServletResponse.class);
        StringWriter writer = mockWriter(response);
        result.writeTo(response);
        Response responseMessage = newResponse();

        assertEquals(toCompactJson(responseMessage), writer.toString());
    }

    @Test
    @DisplayName("write OK response upon cancelling subscription")
    void cancelSubscription() throws IOException {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscription subscription = newSubscription(topic);

        SubscriptionCancelResult result = bridge.cancel(subscription);

        ServletResponse response = mock(ServletResponse.class);
        StringWriter writer = mockWriter(response);
        result.writeTo(response);
        Response responseMessage = newResponse();

        assertEquals(toCompactJson(responseMessage), writer.toString());
    }

    @Test
    @DisplayName("set firebase path to Subscription ID upon subscribe")
    void subscribe() throws IOException {
        Topic topic = topicFactory.forTarget(newTarget());

        SubscribeResult result = bridge.subscribe(topic);

        ServletResponse response = mock(ServletResponse.class);
        StringWriter writer = mockWriter(response);
        result.writeTo(response);
        Subscription subscription = fromJson(writer.toString(), Subscription.class);

        assertEquals(topic, subscription.getTopic());
        assertSubscriptionPointsToFirebase(subscription.getId(), topic);
    }
}
