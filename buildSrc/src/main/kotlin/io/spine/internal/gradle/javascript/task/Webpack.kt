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

import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * Configures `assembleJs` task and creates `copyBundledJs` task to work with `webpack` bundler.
 *
 * Please note, this task group depends on [assemble] and [publish] tasks. Therefore, those tasks
 * should be applied in the first place.
 *
 * In particular, this method:
 *
 *  1. Extends `assembleJs` task to bundle sources during assembling.
 *  2. Creates `copyBundledJs` task and binds it to `prepareJsPublication` task execution.
 *
 * An example of how to apply it in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.javascript.javascript
 * import io.spine.internal.gradle.javascript.task.assemble
 * import io.spine.internal.gradle.javascript.task.publish
 * import io.spine.internal.gradle.javascript.task.webpack
 *
 * // ...
 *
 * javascript {
 *     tasks {
 *         assemble()
 *         publish()
 *         webpack()
 *     }
 * }
 * ```
 */
fun JsTasks.webpack() {

    assembleJs.configure {

        outputs.dir(webpackOutput)

        doLast {
            npm("run", "build")
            npm("run", "build-dev")
        }
    }

    // TODO:2019-02-05:dmytro.grankin: Temporarily don't publish a bundle.
    // See: https://github.com/SpineEventEngine/web/issues/61

    copyBundledJs()/*.also {
        prepareJsPublication.configure {
            dependsOn(it)
        }
    }*/
}

/**
 * Locates `copyBundledJs` task in this [TaskContainer].
 *
 * The task copies bundled JavaScript sources to the publication directory.
 */
internal val TaskContainer.copyBundledJs: TaskProvider<Copy>
    get() = named<Copy>("copyBundledJs")

private fun JsTasks.copyBundledJs() =
    register<Copy>("copyBundledJs") {

        description = "Copies bundled JavaScript sources to the NPM publication directory."
        group = jsPublishTask

        from(assembleJs.map { it.outputs })
        into(webpackPublicationDir)
    }
