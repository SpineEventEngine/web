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

package io.spine.internal.gradle.typescript

import io.spine.internal.gradle.typescript.plugin.TsPlugins
import io.spine.internal.gradle.typescript.task.TsTasks
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType

/**
 * Configures [TsExtension] that facilitates configuration of Gradle tasks and plugins
 * to build TypeScript projects.
 *
 * The whole structure of the extension looks as follows:
 *
 * ```
 * typescript {
 *     environment {
 *         // ...
 *     }
 *     tasks {
 *         // ...
 *     }
 *     plugins {
 *         // ...
 *     }
 * }
 * ```
 *
 * ### Environment
 *
 * One of the main features of this extension is [TsEnvironment]. Environment describes a module
 * itself, used tools with their input/output files, code generation.
 *
 * The extension is shipped with a pre-configured environment. So, no pre-configuration is required.
 * Most properties in [TsEnvironment] have calculated defaults right in the interface.
 * Only two properties need explicit override.
 *
 * The extension defines them as follows:
 *
 *  1. [TsEnvironment.projectDir] –> `project.projectDir`.
 *  2. [TsEnvironment.moduleVersion] —> `project.extra["versionToPublishTs"]`.
 *
 * There are two ways to modify the environment:
 *
 *  1. Update [TsEnvironment] directly. Go with this option when it is a global change
 *     that should affect all projects which use this extension.
 *  2. Use [TsEnvironment.environment] scope — for temporary and custom overridings.
 *
 * An example of a property overriding:
 *
 * ```
 * typescript {
 *     environment {
 *         moduleVersion = "$moduleVersion-SPECIAL_EDITION"
 *     }
 * }
 * ```
 *
 * Please note, environment should be set up firstly to have the effect on the parts
 * of the extension that use it.
 *
 * ### Tasks and Plugins
 *
 * The spirit of tasks configuration in this extension is extracting the code that defines and
 * registers tasks into extension functions upon `TsTasks` in `buildSrc`. Those extensions should be
 * named after a task it registers or a task group if several tasks are registered at once.
 * Then this extension is called in a project's `build.gradle.kts`.
 *
 * `TsTasks` and `TsPlugins` scopes extend [TsContext] which provides access
 * to the current [TsEnvironment] and shortcuts for running `npm` tool.
 *
 * Below is the simplest example of how to create a primitive `printNpmVersion` task.
 *
 * Firstly, a corresponding extension function should be defined in `buildSrc`:
 *
 * ```
 * fun TsTasks.printNpmVersion() =
 *     register("printNpmVersion") {
 *         doLast {
 *             npm("--version")
 *         }
 *     }
 * ```
 *
 * Secondly, in a project's `build.gradle.kts` this extension is called:
 *
 * ```
 * typescript {
 *     tasks {
 *         printNpmVersion()
 *     }
 * }
 * ```
 *
 * An extension function is not restricted to register exactly one task. If several tasks can
 * be grouped into a logical bunch, they should be registered together:
 *
 * ```
 * fun TsTasks.build() {
 *     assembleTs()
 *     testTs()
 *     generateCoverageReport()
 * }
 *
 * private fun TsTasks.assembleTs() = ...
 *
 * private fun TsTasks.testTs() = ...
 *
 * private fun TsTasks.generateCoverageReport() = ...
 * ```
 *
 * This section is mostly dedicated to tasks. But tasks and plugins are configured
 * in a very similar way. So, everything above is also applicable to plugins. More detailed
 * guides can be found in docs to `TsTasks` and `TsPlugins`.
 *
 * @see [ConfigurableTsEnvironment]
 * @see [TsTasks]
 * @see [TsPlugins]
 */
fun Project.typescript(configuration: TsExtension.() -> Unit) {
    extensions.run {
        configuration.invoke(
            findByType() ?: create("tsExtension", project)
        )
    }
}

/**
 * Scope for performing TypeScript-related configuration.
 *
 * @see [typescript]
 */
open class TsExtension(internal val project: Project) {

    private val configurableEnvironment = ConfigurableTsEnvironment(
        object : TsEnvironment {
            override val projectDir = project.projectDir
            override val moduleVersion = project.extra["versionToPublishTs"].toString()
        }
    )

    val environment: TsEnvironment = configurableEnvironment
    val tasks: TsTasks = TsTasks(environment, project)
    val plugins: TsPlugins = TsPlugins(environment, project)

    /**
     * Overrides default values of [TsEnvironment].
     *
     * Please note, environment should be set up firstly to have the effect on the parts
     * of the extension that use it.
     */
    fun environment(overridings: ConfigurableTsEnvironment.() -> Unit) =
        configurableEnvironment.run(overridings)

    /**
     * Configures [TypeScript-related tasks][TsTasks].
     */
    fun tasks(configurations: TsTasks.() -> Unit) =
        tasks.run(configurations)

    /**
     * Configures [TypeScript-related plugins][TsPlugins].
     */
    fun plugins(configurations: TsPlugins.() -> Unit) =
        plugins.run(configurations)

}
