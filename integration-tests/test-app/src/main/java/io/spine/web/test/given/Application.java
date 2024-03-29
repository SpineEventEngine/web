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

package io.spine.web.test.given;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.protobuf.util.Durations;
import io.spine.io.Resource;
import io.spine.server.BoundedContext;
import io.spine.server.CommandService;
import io.spine.server.QueryService;
import io.spine.server.SubscriptionService;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.query.FirebaseQueryBridge;
import io.spine.web.firebase.subscription.FirebaseSubscriptionBridge;
import io.spine.web.query.BlockingQueryBridge;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.web.firebase.FirebaseClientFactory.remoteClient;
import static io.spine.web.firebase.FirebaseCredentials.fromGoogleCredentials;

/**
 * A test Spine application.
 */
final class Application {

    private static final String DATABASE_URL = "https://spine-dev.firebaseio.com/";

    private final CommandService commandService;
    private final FirebaseQueryBridge fbQueryBridge;
    private final FirebaseSubscriptionBridge subscriptionBridge;
    private final BlockingQueryBridge blockingQueryBridge;

    private Application(CommandService commandService,
                        QueryService queryService,
                        SubscriptionService subscriptionService,
                        FirebaseClient client) {
        this.commandService = commandService;
        this.fbQueryBridge = FirebaseQueryBridge
                .newBuilder()
                .setQueryService(queryService)
                .setFirebaseClient(client)
                .build();
        this.blockingQueryBridge = new BlockingQueryBridge(queryService);
        this.subscriptionBridge = FirebaseSubscriptionBridge
                .newBuilder()
                .setSubscriptionService(subscriptionService)
                .setMaxProlongation(Durations.fromMinutes(30))
                .setFirebaseClient(client)
                .build();
    }

    static Application create(BoundedContext tasksContext, BoundedContext usersContext) {
        checkNotNull(tasksContext);
        checkNotNull(usersContext);
        var commandService = CommandService.newBuilder()
                .add(tasksContext)
                .add(usersContext)
                .build();
        var queryService = QueryService.newBuilder()
                .add(tasksContext)
                .add(usersContext)
                .build();
        var subscriptionService = SubscriptionService.newBuilder()
                .add(tasksContext)
                .add(usersContext)
                .build();
        var retryingClient = buildClient();
        return new Application(commandService, queryService, subscriptionService, retryingClient);
    }

    private static FirebaseClient buildClient() {
        var googleCredentialsFile = Resource.file(
                "spine-dev.json", Application.class.getClassLoader()
        );

        GoogleCredentials credentials;
        try {
            credentials = GoogleCredentials.fromStream(googleCredentialsFile.open());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        var options = FirebaseOptions.builder()
                .setDatabaseUrl(DATABASE_URL)
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);

        var database = FirebaseDatabase.getInstance();
        var firebaseCredentials = fromGoogleCredentials(credentials);
        var firebaseClient = remoteClient(database, firebaseCredentials);
        return new TidyClient(firebaseClient);
    }

    CommandService commandService() {
        return commandService;
    }

    FirebaseQueryBridge firebaseQueryBridge() {
        return this.fbQueryBridge;
    }

    BlockingQueryBridge blockingQueryBridge() {
        return this.blockingQueryBridge;
    }

    FirebaseSubscriptionBridge subscriptionBridge() {
        return this.subscriptionBridge;
    }
}
