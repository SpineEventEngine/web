/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.internal.gradle.typescript.task

import io.spine.internal.gradle.typescript.TsContext
import io.spine.internal.gradle.typescript.TsEnvironment
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer

/**
 * A scope for registering and configuring TypeScript-related tasks.
 *
 * The scope provides:
 *
 *  1. Access to the current [TsContext].
 *  2. Project's [TaskContainer].
 *  3. Default task groups.
 *
 * Supposing, one needs to create a new task that would participate in building. Let the task name
 * be `bundleTs`. To do that, several steps should be completed:
 *
 *  1. Define the task name and type using [TaskName][io.spine.internal.gradle.TaskName].
 *  2. Create a public typed reference for the task upon [TaskContainer]. It would facilitate
 *      referencing to the new task, so that external tasks could depend on it. This reference
 *      should be documented.
 *  3. Implement an extension upon [TsTasks] to register the task.
 *  4. Call the resulted extension from `build.gradle.kts`.
 *
 * Here's an example of `bundleTs()` extension:
 *
 * ```
 * import io.spine.internal.gradle.named
 * import io.spine.internal.gradle.register
 * import io.spine.internal.gradle.TaskName
 * import org.gradle.api.Task
 * import org.gradle.api.tasks.TaskContainer
 * import org.gradle.api.tasks.Exec
 *
 * // ...
 *
 * private val bundleTsName = TaskName.of("bundleTs", Exec::class)
 *
 * /**
 *  * Locates `bundleTs` task in this [TaskContainer].
 *  *
 *  * The task bundles TypeScript sources using `webpack` tool.
 *  */
 * val TaskContainer.bundleTs: TaskProvider<Exec>
 *     get() = named(bundleTsName)
 *
 * fun TsTasks.bundleTs() =
 *     register(bundleTsName) {
 *
 *         description = "Bundles TypeScript sources using `webpack` tool."
 *         group = TsTasks.Group.build
 *
 *         // ...
 *     }
 * ```
 *
 * And here's how to apply it in `build.gradle.kts`:
 *
 * ```
 * import io.spine.internal.gradle.typescript.typescript
 * import io.spine.internal.gradle.typescript.task.bundleTs
 *
 * // ...
 *
 * typescript {
 *     tasks {
 *         bundleTs()
 *     }
 * }
 * ```
 *
 * Declaring typed references upon [TaskContainer] is optional. But it is highly encouraged
 * to reference other tasks by such extensions instead of hard-typed string values.
 */
class TsTasks(tsEnv: TsEnvironment, project: Project) : TsContext(tsEnv, project),
    TaskContainer by project.tasks {
    /**
     * Default task groups for tasks that participate in building a TypeScript module.
     *
     * @see [org.gradle.api.Task.getGroup]
     */
    internal object Group {
        const val assemble = "TypeScript/Assemble"
        const val check = "TypeScript/Check"
        const val clean = "TypeScript/Clean"
        const val build = "TypeScript/Build"
        const val publish = "TypeScript/Publish"
    }
}
