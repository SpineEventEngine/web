/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.core.Response;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.SubscriptionService;
import io.spine.web.firebase.given.MemoizingFirebase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.core.Status.StatusCase.OK;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.assertSubscriptionPointsToFirebase;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newBridge;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newResponse;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newSubscription;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newTarget;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.topicFactory;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`FirebaseSubscriptionBridge` should")
class FirebaseSubscriptionBridgeTest {

    private FirebaseSubscriptionBridge bridge;
    private TopicFactory topicFactory;

    @BeforeEach
    void setUp() {
        SubscriptionServiceImplBase subscriptionService = new TestSubscriptionService();
        bridge = newBridge(MemoizingFirebase.withNoLatency(), subscriptionService);
        topicFactory = topicFactory();
    }

    @Test
    @DisplayName("require Query Service set in Builder")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Method called to throw.
    void requireQueryService() {
        FirebaseSubscriptionBridge.Builder builder = FirebaseSubscriptionBridge
                .newBuilder()
                .setFirebaseClient(MemoizingFirebase.withNoLatency());
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("require Firebase Client set in Builder")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckReturnValue"}) // Method called to throw.
    void requireFirebaseClient() {
        SubscriptionService subscriptionService = SubscriptionService
                .newBuilder()
                .add(BoundedContextBuilder.assumingTests().build())
                .build();
        FirebaseSubscriptionBridge.Builder builder = FirebaseSubscriptionBridge
                .newBuilder()
                .setSubscriptionService(subscriptionService);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("write OK response upon subscription keep up")
    void keepUpSubscription() {
        Topic topic = topicFactory.forTarget(newTarget());
        FirebaseSubscription subscription = bridge.subscribe(topic);
        Response keptUp = bridge.keepUp(subscription.getSubscription());
        Response responseMessage = newResponse();
        assertThat(keptUp)
                .isEqualTo(responseMessage);
    }

    @Test
    @DisplayName("write an error response upon unknown subscription keep up")
    void failKeepUp() {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscription subscription = newSubscription(topic);
        Response keptUp = bridge.keepUp(subscription);
        assertThat(keptUp.getStatus().getStatusCase())
                .isEqualTo(ERROR);
    }

    @Test
    @DisplayName("write OK response upon cancelling subscription")
    void cancelSubscription() {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscription subscription = newSubscription(topic);

        FirebaseSubscription subscriptionResult = bridge.subscribe(topic);
        assertThat(subscriptionResult).isNotNull();
        Response canceled = bridge.cancel(subscription);
        Response responseMessage = newResponse();

        assertThat(canceled)
                .isEqualTo(responseMessage);
    }

    @Test
    @DisplayName("write an error response upon cancelling an unknown subscription")
    void failCancellation() {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscription subscription = newSubscription(topic);
        Response canceled = bridge.cancel(subscription);
        assertThat(canceled.getStatus().getStatusCase())
                .isEqualTo(ERROR);
    }

    @Test
    @DisplayName("write an error response upon cancelling a subscription twice")
    void failDoubleCancellation() {
        Topic topic = topicFactory.forTarget(newTarget());
        FirebaseSubscription subscriptionResult = bridge.subscribe(topic);
        assertThat(subscriptionResult)
                .isNotNull();

        Response canceled = bridge.cancel(subscriptionResult.getSubscription());
        assertThat(canceled.getStatus().getStatusCase())
                .isEqualTo(OK);

        Response canceledAgain = bridge.cancel(subscriptionResult.getSubscription());
        assertThat(canceledAgain.getStatus().getStatusCase())
                .isEqualTo(ERROR);
    }

    @Test
    @DisplayName("set firebase path to Subscription ID upon subscribe")
    void subscribe() {
        Topic topic = topicFactory.forTarget(newTarget());

        FirebaseSubscription firebaseSubscription = bridge.subscribe(topic);
        Subscription subscription = firebaseSubscription.getSubscription();
        assertThat(subscription.getTopic())
                .isEqualTo(topic);
        assertSubscriptionPointsToFirebase(firebaseSubscription.getNodePath(), topic);
    }
}
