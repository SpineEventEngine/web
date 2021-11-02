/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.SubscriptionService;
import io.spine.type.TypeUrl;
import io.spine.web.Cancel;
import io.spine.web.SubscriptionsCancelled;
import io.spine.web.KeepUp;
import io.spine.web.KeepUpOutcome;
import io.spine.web.SubscriptionsKeptUp;
import io.spine.web.Subscribe;
import io.spine.web.SubscriptionsCreated;
import io.spine.web.WebSubscription;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.given.MemoizingFirebase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.Durations.fromHours;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.core.Status.StatusCase.OK;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.web.KeepUpOutcome.KindCase.NEW_VALID_THRU;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.assertSubscriptionPointsToFirebase;
import static io.spine.web.firebase.given.FirebaseSubscriptionBridgeTestEnv.newBridge;
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
        Subscribe subscribe = Subscribe.newBuilder()
                .addTopic(topic)
                .setLifespan(fromHours(1))
                .build();
        SubscriptionsCreated subscriptionsCreated = bridge.subscribe(subscribe);
        SubscriptionId subscriptionId = subscriptionsCreated
                .getResultList()
                .get(0)
                .getSubscription()
                .getSubscription()
                .getId();
        KeepUp keepUp = KeepUp.newBuilder()
                .addSubscription(subscriptionId)
                .setProlongBy(fromHours(1))
                .build();
        SubscriptionsKeptUp subscriptionsKeptUp = bridge.keepUp(keepUp);
        assertThat(subscriptionsKeptUp.getOutcome(0).getKindCase())
                .isEqualTo(NEW_VALID_THRU);
    }

    @Test
    @DisplayName("write an error response upon unknown subscription keep up")
    void failKeepUp() {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscription subscription = newSubscription(topic);
        Duration prolongBy = Durations.fromDays(2);
        KeepUp keepUp = KeepUp.newBuilder()
                .addSubscription(subscription.getId())
                .setProlongBy(prolongBy)
                .build();
        SubscriptionsKeptUp subscriptionsKeptUp = bridge.keepUp(keepUp);
        assertThat(subscriptionsKeptUp.getOutcome(0).getKindCase())
                .isEqualTo(KeepUpOutcome.KindCase.ERROR);
    }

    @Test
    @DisplayName("write OK response upon cancelling subscription")
    void cancelSubscription() {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscribe subscribe = Subscribe.newBuilder()
                .addTopic(topic)
                .setLifespan(fromHours(1))
                .build();
        SubscriptionsCreated subscriptionsCreated = bridge.subscribe(subscribe);
        assertThat(subscriptionsCreated)
                .isNotNull();
        SubscriptionId id = subscriptionsCreated.getResult(0)
                                                .getSubscription()
                                                .getSubscription()
                                                .getId();
        Cancel cancel = Cancel
                .newBuilder()
                .addSubscription(id)
                .build();
        SubscriptionsCancelled subscriptionsCancelled = bridge.cancel(cancel);
        assertThat(subscriptionsCancelled.getAck(0).getStatus().getStatusCase())
                .isEqualTo(OK);
    }

    @Test
    @DisplayName("write an error response upon cancelling an unknown subscription")
    void failCancellation() {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscription subscription = newSubscription(topic);
        Cancel cancel = Cancel.newBuilder()
                .addSubscription(subscription.getId())
                .build();
        SubscriptionsCancelled response = bridge.cancel(cancel);
        assertThat(response.getAck(0).getStatus().getStatusCase())
                .isEqualTo(ERROR);
    }

    @Test
    @DisplayName("write an error response upon cancelling a subscription twice")
    void failDoubleCancellation() {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscribe subscribe = Subscribe.newBuilder()
                .addTopic(topic)
                .setLifespan(fromHours(1))
                .build();
        SubscriptionsCreated subscriptionsCreated = bridge.subscribe(subscribe);
        assertThat(subscriptionsCreated)
                .isNotNull();
        WebSubscription webSubscription = subscriptionsCreated
                .getResult(0)
                .getSubscription();
        assertThat(webSubscription.getExtraList())
                .hasSize(1);
        assertThat(webSubscription.getExtra(0).getTypeUrl())
                .isEqualTo(TypeUrl.of(NodePath.class).value());
        Cancel cancel = Cancel.newBuilder()
                .addSubscription(webSubscription.getSubscription().getId())
                .build();
        SubscriptionsCancelled cancelled = bridge.cancel(cancel);
        assertThat(cancelled.getAck(0).getStatus().getStatusCase())
                .isEqualTo(OK);

        SubscriptionsCancelled subscriptionsCancelledAgain = bridge.cancel(cancel);
        assertThat(subscriptionsCancelledAgain.getAck(0).getStatus().getStatusCase())
                .isEqualTo(ERROR);
    }

    @Test
    @DisplayName("set firebase path to Subscription ID upon subscribe")
    void subscribe() {
        Topic topic = topicFactory.forTarget(newTarget());
        Subscribe request = Subscribe.newBuilder()
                .addTopic(topic)
                .build();
        SubscriptionsCreated subscriptionsCreated = bridge.subscribe(request);
        WebSubscription webSubscription = subscriptionsCreated.getResult(0)
                                                              .getSubscription();
        Subscription subscription = webSubscription.getSubscription();
        assertThat(subscription.getTopic())
                .isEqualTo(topic);

        NodePath nodePath = unpack(webSubscription.getExtra(0), NodePath.class);
        assertSubscriptionPointsToFirebase(nodePath, topic);
    }
}
