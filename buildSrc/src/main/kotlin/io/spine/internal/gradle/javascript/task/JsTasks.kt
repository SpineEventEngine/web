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

import io.spine.internal.gradle.javascript.JsEnvironment
import io.spine.internal.gradle.javascript.JsContext
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer

/**
 * A scope for working with JavaScript-related tasks.
 *
 * The context provides:
 *
 *  1. Access to the current [JsContext];
 *  2. Project's [TaskContainer];
 *  3. Default task groups.
 *
 * From this scope one can [register][JsTasks.register] new tasks and
 * [configure][JsTasks.configure] already present tasks.
 *
 * Supposing, one needs to create a new task that would participate in building. Let task name be
 * `bundleJs`. To achieve the objection, several steps are to be performed:
 *
 *  1. Define the task as an extension function upon `JsTaskRegistering` scope;
 *  2. Create typed reference for the task upon [TaskContainer]. It would facilitate referencing
 *     to the new task. For example, to add external dependents;
 *  3. Call the resulted extension from `build.gradle.kts`.
 *
 * Here's an example of `bundleJs()` extension:
 *
 * ```
 * import io.spine.internal.gradle.js.task.JsTaskRegistering
 * import org.gradle.api.Task
 * import org.gradle.api.tasks.TaskContainer
 *
 * // ...
 *
 * val TaskContainer.bundleJs: Task
 *     get() = getByName("bundleJs")
 *
 * fun JsTaskRegistering.bundleJs() =
 *     register("bundleJs) {
 *
 *         description = "Bundles js sources using `webpack` tool.`
 *         group = jsBuildTask
 *
 *         doLast {
 *             npm("run", "build")
 *             npm("run", "build-dev")
 *         }
 *
 *         buildJs.dependsOn(this)
 *     }
 * ```
 *
 * And how to apply it in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.js.javascript
 * import io.spine.internal.gradle.js.task.buildJs
 *
 * // ...
 *
 * javascript {
 *     tasks {
 *         register {
 *             bundleJs()
 *         }
 *     }
 * }
 * ```
 *
 * The configuration process looks very similar to registration.
 *
 * Declaring typed references upon [TaskContainer] is optional. But it is highly encouraged
 * to reference to other tasks by such extensions instead of hard-typed string values.
 */
open class JsTasks(jsEnv: JsEnvironment, project: Project)
    : JsContext(jsEnv, project), TaskContainer by project.tasks
{
    internal val jsAssembleTask = "JavaScript/Build"
    internal val jsCheckTask = "JavaScript/Check"
    internal val jsCleanTask = "JavaScript/Clean"
    internal val jsBuildTask = "JavaScript/Build"
    internal val jsPublishTask = "JavaScript/Publish"
}
