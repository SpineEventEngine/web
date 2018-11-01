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

package io.spine.web.test.given;

import io.spine.server.BoundedContext;
import io.spine.server.CommandService;
import io.spine.server.QueryService;
import io.spine.web.firebase.FirebaseClient;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.web.firebase.FirebaseClients.rest;

/**
 * A test Spine application.
 *
 * @author Dmytro Dashenkov
 */
final class Application {

    private static final String DATABASE_URL = "https://spine-dev.firebaseio.com/";

    private final CommandService commandService;
    private final QueryService queryService;
    private final FirebaseClient firebaseClient;

    private Application(CommandService commandService,
                        QueryService queryService,
                        FirebaseClient client) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.firebaseClient = client;
    }

    static Application create(BoundedContext boundedContext) {
        checkNotNull(boundedContext);
        CommandService commandService = CommandService.newBuilder()
                                                      .add(boundedContext)
                                                      .build();
        QueryService queryService = QueryService.newBuilder()
                                                .add(boundedContext)
                                                .build();
        FirebaseClient firebaseClient = rest(DATABASE_URL);
        return new Application(commandService, queryService, firebaseClient);
    }

    CommandService commandService() {
        return commandService;
    }

    QueryService queryService() {
        return queryService;
    }

    FirebaseClient firebaseClient() {
        return firebaseClient;
    }
}
