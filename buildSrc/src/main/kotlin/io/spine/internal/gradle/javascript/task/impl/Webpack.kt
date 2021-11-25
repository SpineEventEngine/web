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

package io.spine.internal.gradle.javascript.task.impl

import io.spine.internal.gradle.javascript.task.JsTaskConfiguring
import io.spine.internal.gradle.javascript.task.JsTaskRegistering
import io.spine.internal.gradle.javascript.task.assembleJs
import io.spine.internal.gradle.javascript.task.testJs
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create

fun JsTaskRegistering.webpack() {
    copyBundledJs()
}

/**
 * Extends `buildJs` and `testJs` to run `webpack` builds.
 */
fun JsTaskConfiguring.webPack() {

    // Customizes `assembleJs` task with running `webpack` bundler.

    assembleJs.apply {

        outputs.dir(webPackOutput)

        doLast {
            npm("run", "build")
            npm("run", "build-dev")
        }
    }

    // Customizes `testJs` task with running JavaScript tests.

    testJs.apply {

        doLast {
            npm("run", "test")
        }
    }
}

private fun JsTaskRegistering.copyBundledJs() =
    create<Copy>("copyBundledJs") {

        description = "Copies bundled JavaScript sources to the NPM publication directory."
        group = jsAnyTask

        from(assembleJs.outputs)
        into(webPackPublicationDir)
    }
