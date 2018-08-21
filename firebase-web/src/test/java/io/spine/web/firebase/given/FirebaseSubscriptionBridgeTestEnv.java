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

package io.spine.web.firebase.given;

import com.google.common.base.Joiner;
import com.google.firebase.database.FirebaseDatabase;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.SubscriptionIdVBuilder;
import io.spine.client.SubscriptionVBuilder;
import io.spine.client.Target;
import io.spine.client.TargetVBuilder;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.client.grpc.QueryServiceGrpc.QueryServiceImplBase;
import io.spine.core.Response;
import io.spine.core.ResponseVBuilder;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.web.firebase.FirebaseSubscriptionBridge;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.Responses.statusOk;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Mykhailo Drachuk
 */
public final class FirebaseSubscriptionBridgeTestEnv {

    private static final Pattern ILLEGAL_DATABASE_PATH_SYMBOL = Pattern.compile("[\\[\\].$#]");
    private static final String SUBSTITUTION_SYMBOL = "-";
    private static final String PATH_DELIMITER = "/";
    private static final String DEFAULT_TENANT = "common";

    /**
     * Prevents instantiation of this test environment.
     */
    private FirebaseSubscriptionBridgeTestEnv() {
    }

    public static void assertSubscriptionPointsToFirebase(SubscriptionId id, Topic topic) {
        String tenantId = tenantIdAsString(topic);
        String actor = actorAsString(topic);
        Collection<String> pathElements = newArrayList();
        if (!tenantId.isEmpty()) {
            pathElements.add(escaped(tenantId));
        }
        if (!actor.isEmpty()) {
            pathElements.add(escaped(actor));
        }
        String expectedPath = Joiner.on(PATH_DELIMITER)
                                    .join(pathElements) + PATH_DELIMITER;
        String path = id.getValue();
        assertTrue(path.startsWith(expectedPath));
    }

    @SuppressWarnings("UnnecessaryDefault")
    private static String tenantIdAsString(Topic topic) {
        TenantId tenantId = topic.getContext()
                                 .getTenantId();
        TenantId.KindCase kind = tenantId.getKindCase();
        switch (kind) {
            case EMAIL:
                return tenantId.getEmail()
                               .getValue();
            case DOMAIN:
                return tenantId.getDomain()
                               .getValue();
            case VALUE:
                return tenantId.getValue();
            case KIND_NOT_SET: // Fallthrough intended.
            default:
                return DEFAULT_TENANT;
        }
    }

    private static String actorAsString(Topic topic) {
        UserId actor = topic.getContext()
                            .getActor();
        String result = actor.getValue();
        return result;
    }

    private static String escaped(String dirty) {
        return ILLEGAL_DATABASE_PATH_SYMBOL.matcher(dirty)
                                           .replaceAll(SUBSTITUTION_SYMBOL);
    }

    public static Response newResponse() {
        return ResponseVBuilder.newBuilder()
                               .setStatus(statusOk())
                               .build();
    }

    public static StringWriter mockWriter(ServletResponse response) throws IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        return stringWriter;
    }

    public static Subscription newSubscription(Topic topic) {
        return SubscriptionVBuilder.newBuilder()
                                   .setId(subscriptionId())
                                   .setTopic(topic)
                                   .build();
    }

    public static Target newTarget() {
        return TargetVBuilder.newBuilder()
                             .setType("test-type")
                             .setIncludeAll(true)
                             .build();
    }

    public static FirebaseSubscriptionBridge newBridge(FirebaseDatabase firebaseDatabase,
                                                       QueryServiceImplBase queryService) {
        return FirebaseSubscriptionBridge.newBuilder()
                                         .setDatabase(firebaseDatabase)
                                         .setQueryService(queryService)
                                         .setWriteAwaitSeconds(5)
                                         .build();
    }

    public static TopicFactory topicFactory() {
        return TestActorRequestFactory.newInstance(UserId.newBuilder()
                                                         .setValue("test-user")
                                                         .build())
                                      .topic();
    }

    private static SubscriptionId subscriptionId() {
        return SubscriptionIdVBuilder.newBuilder()
                                     .setValue("test-subscription")
                                     .build();
    }
}
