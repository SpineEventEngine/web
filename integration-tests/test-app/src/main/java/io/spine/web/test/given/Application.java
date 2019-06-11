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

package io.spine.web.test.given;

import io.spine.server.BoundedContext;
import io.spine.server.CommandService;
import io.spine.server.QueryService;
import io.spine.web.firebase.DatabaseUrl;
import io.spine.web.firebase.DatabaseUrls;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.query.FirebaseQueryBridge;
import io.spine.web.firebase.subscription.FirebaseSubscriptionBridge;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A test Spine application.
 */
final class Application {

    private static final DatabaseUrl DATABASE_URL =
            DatabaseUrls.from("http://127.0.0.1:5000/");

    private final CommandService commandService;
    private final FirebaseQueryBridge queryBridge;
    private final FirebaseSubscriptionBridge subscriptionBridge;

    private Application(CommandService commandService,
                        QueryService queryService,
                        FirebaseClient client) {
        this.commandService = commandService;
        this.queryBridge = FirebaseQueryBridge.newBuilder()
                                              .setQueryService(queryService)
                                              .setFirebaseClient(client)
                                              .build();
        this.subscriptionBridge = FirebaseSubscriptionBridge.newBuilder()
                                                            .setQueryService(queryService)
                                                            .setFirebaseClient(client)
                                                            .build();
    }

    static Application create(BoundedContext boundedContext) {
        checkNotNull(boundedContext);
        CommandService commandService = CommandService.newBuilder()
                                                      .add(boundedContext)
                                                      .build();
        QueryService queryService = QueryService.newBuilder()
                                                .add(boundedContext)
                                                .build();
        FirebaseClient firebaseClient = new FirebaseApacheClient(DATABASE_URL);
        return new Application(commandService, queryService, firebaseClient);
    }

    CommandService commandService() {
        return commandService;
    }

    FirebaseQueryBridge queryBridge() {
        return this.queryBridge;
    }

    FirebaseSubscriptionBridge subscriptionBridge() {
        return this.subscriptionBridge;
    }
}
