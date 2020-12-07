/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.web.firebase.given;

import com.google.common.base.Joiner;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Target;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceImplBase;
import io.spine.core.Response;
import io.spine.core.UserId;
import io.spine.server.model.Nothing;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.type.TypeUrl;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.subscription.FirebaseSubscriptionBridge;

import java.util.Collection;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.core.Responses.statusOk;

@SuppressWarnings("DuplicateStringLiteralInspection") // Duplicate strings for testing.
public final class FirebaseSubscriptionBridgeTestEnv {

    private static final Pattern ILLEGAL_DATABASE_PATH_SYMBOL = Pattern.compile("[\\[\\].$#]");
    private static final String SUBSTITUTION_SYMBOL = "-";
    private static final String PATH_DELIMITER = "/";
    private static final String DEFAULT_TENANT = "common";
    private static final Joiner PATH_JOINER = Joiner.on(PATH_DELIMITER);

    /**
     * Prevents instantiation of this test environment.
     */
    private FirebaseSubscriptionBridgeTestEnv() {
    }

    public static void assertSubscriptionPointsToFirebase(NodePath path, Topic topic) {
        String actor = actorAsString(topic);
        Collection<String> pathElements = newArrayList(
                DEFAULT_TENANT, escaped(actor), topic.getId().getValue()
        );
        String expectedPath = PATH_JOINER.join(pathElements);
        assertThat(path.getValue())
                .isEqualTo(expectedPath);
    }

    private static String actorAsString(Topic topic) {
        UserId actor = topic.getContext()
                            .getActor();
        return actor.getValue();
    }

    private static String escaped(String dirty) {
        return ILLEGAL_DATABASE_PATH_SYMBOL.matcher(dirty)
                                           .replaceAll(SUBSTITUTION_SYMBOL);
    }

    public static Response newResponse() {
        return Response
                .newBuilder()
                .setStatus(statusOk())
                .vBuild();
    }

    public static Subscription newSubscription(Topic topic) {
        return Subscription
                .newBuilder()
                .setId(subscriptionId())
                .setTopic(topic)
                .vBuild();
    }

    public static Target newTarget() {
        return Target
                .newBuilder()
                .setType(TypeUrl.of(Nothing.getDefaultInstance()).value())
                .setIncludeAll(true)
                .vBuild();
    }

    public static FirebaseSubscriptionBridge
    newBridge(FirebaseClient firebaseClient,
              SubscriptionServiceImplBase subscriptionService) {
        return FirebaseSubscriptionBridge
                .newBuilder()
                .setFirebaseClient(firebaseClient)
                .setSubscriptionService(subscriptionService)
                .build();
    }

    public static TopicFactory topicFactory() {
        UserId userId = UserId
                .newBuilder()
                .setValue("test-user")
                .vBuild();
        return new TestActorRequestFactory(userId).topic();
    }

    private static SubscriptionId subscriptionId() {
        return SubscriptionId
                .newBuilder()
                .setValue("test-subscription")
                .vBuild();
    }
}
