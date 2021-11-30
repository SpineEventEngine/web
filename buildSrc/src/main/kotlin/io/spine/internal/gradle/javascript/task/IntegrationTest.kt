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

package io.spine.internal.gradle.javascript.task

import io.spine.internal.gradle.base.build
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

/**
 * Locates `linkSpineWebModule` task in this [TaskContainer].
 *
 * The task installs unpublished artifact of `spine-web` library as a module dependency.
 *
 * Creates a symbolic link from globally-installed `spine-web` library to `node_modules` of
 * the current project.
 *
 * See also: [npm-link | npm Docs](https://docs.npmjs.com/cli/v8/commands/npm-link)
 */
val TaskContainer.linkSpineWebModule: Task
    get() = getByName("linkSpineWebModule")

fun JsTaskRegistering.linkSpineWebModule() =
    register("linkSpineWebModule") {

        description = "Install unpublished artifact of `spine-web` library as a module dependency."
        group = jsAssembleTask

        dependsOn(":client-js:publishJsLocally")

        doLast {
            npm("run", "installLinkedLib")
        }
    }


/**
 * Locates `integrationTest` task in this [TaskContainer].
 *
 * The task runs integration tests of the `spine-web` library
 * against a sample Spine-based application.
 *
 * A sample Spine-based application is run from the `test-app` module before integration
 * tests and is stopped as the tests complete.
 *
 * See also: `./integration-tests/README.MD`
 */
val TaskContainer.integrationTest: Task
    get() = getByName("integrationTest")

fun JsTaskRegistering.integrationTest() =
    register("integrationTest") {

        // TODO:2019-05-29:yegor.udovchenko
        // Find a way to run the same tests against `spine-web` source code
        // in `client-js` module to recover coverage.
        // See issue: https://github.com/SpineEventEngine/web/issues/96

        description = "Runs integration tests of the `spine-web` library " +
                "against the sample application."
        group = jsCheckTask

        dependsOn(build, linkSpineWebModule, ":test-app:appBeforeIntegrationTest")

        doLast {
            npm("run", "test")
        }

        finalizedBy(":test-app:appAfterIntegrationTest")
    }
