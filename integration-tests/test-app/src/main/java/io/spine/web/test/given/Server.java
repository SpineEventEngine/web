/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import io.spine.server.BoundedContext;
import io.spine.server.DefaultRepository;

/**
 * The test application server.
 */
final class Server {

    private static final Application app = createApplication();

    /**
     * Prevents the utility class instantiation.
     */
    private Server() {
    }

    /**
     * Retrieves the {@link Application} instance.
     */
    static Application application() {
        return app;
    }

    private static Application createApplication() {
        BoundedContext users = BoundedContext
                .singleTenant("Users Context")
                .add(DefaultRepository.of(UserInfoAggregate.class))
                .add(DefaultRepository.of(UserInfoProjection.class))
                .build();
        BoundedContext tasks = BoundedContext
                .multitenant("Tasks Context")
                .add(DefaultRepository.of(TaskAggregate.class))
                .add(DefaultRepository.of(ProjectAggregate.class))
                .add(new UserTasksProjectionRepository())
                .addCommandFilter(new CreateTaskCommandFilter())
                .build();
        Application app = Application.create(tasks, users);
        return app;
    }
}
