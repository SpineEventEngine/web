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

package io.spine.internal.gradle.javascript

import io.spine.internal.gradle.javascript.task.JsTasks
import io.spine.internal.gradle.javascript.plugin.JsPlugins
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType

/**
 * This scope facilitates configuration of Gradle tasks and plugins to build JavaScripts projects.
 *
 * The whole structure of the scope looks as follows:
 *
 * ```
 * javascript {
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
 * One of the main features of this scope is [JsEnvironment]. Environment describes a module itself,
 * used tools with their input/output files, code generation.
 *
 * The scope is shipped with a pre-configured environment. So, no pre-configuration is required.
 * Most values in [JsEnvironment] have calculated defaults. Only two of them need explicit override.
 *
 * The scope defines them as follows:
 *
 *  1. [JsEnvironment.projectDir] –> `project.projectDir`;
 *  2. [JsEnvironment.moduleVersion] —> `project.extra["versionToPublishJs"]`.
 *
 * There two ways to modify the environment:
 *
 *  1. Update [JsEnvironment] directly. Go with this option when it is a global change
 *     that should affect all projects which use this extension;
 *  2. Use [JsExtension.environment] scope — for temporary and custom overriding.
 *
 * An example of a property overriding:
 *
 * ```
 * javascript {
 *     environment {
 *         moduleVersion = "$moduleVersion-SPECIAL_EDITION"
 *     }
 * }
 * ```
 *
 * ### Tasks and Plugins
 *
 * The main spirit of tasks configuration in this scope is extracting procedural code into
 * extension functions upon `JsTasks`. Then calling those functions in concrete `build.gradle.kts`.
 *
 * `JsTasks` and `JsPlugins` scopes extend [JsContext] which provide access
 * to the current [JsEnvironment] and shortcuts for running `npm` command.
 *
 * Below is the simplest example of how to crate `printNpmVersion` task.
 *
 * Firstly, a corresponding extension function should be defined in `buildSrc`:
 *
 * ```
 * fun JsTasks.printNpmVersion() =
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
 * javascript {
 *     tasks {
 *         printNpmVersion()
 *     }
 * }
 * ```
 *
 * This section is mostly dedicated to Tasks. But Tasks and Plugins are configured
 * in a very similar way. So, everything above is also applicable to plugins.
 *
 * @see [ConfigurableJsEnvironment]
 * @see [JsTasks]
 * @see [JsPlugins]
 */
fun Project.javascript(configuration: JsExtension.() -> Unit) {
    extensions.run {
        configuration.invoke(
            findByType() ?: create("jsExtension", project)
        )
    }
}

/**
 * Scope for performing JavaScript-related configuration.
 *
 * @see [javascript]
 */
open class JsExtension(internal val project: Project) {

    private val configurableEnvironment = ConfigurableJsEnvironment(
        object : JsEnvironment {
            override val projectDir = project.projectDir
            override val moduleVersion = project.extra["versionToPublishJs"].toString()
        }
    )

    val environment: JsEnvironment = configurableEnvironment
    val tasks: JsTasks = JsTasks(environment, project)
    val plugins: JsPlugins = JsPlugins(environment, project)

    /**
     * Overrides default values of [JsEnvironment].
     *
     * Please note, environment should be set up firstly to have the effect on the parts
     * of the extension that use it.
     */
    fun environment(overridings: ConfigurableJsEnvironment.() -> Unit) =
        configurableEnvironment.run(overridings)

    /**
     * Configures [Js-related tasks][JsTasks].
     */
    fun tasks(configurations: JsTasks.() -> Unit) =
        tasks.run(configurations)

    /**
     * Configures [Js-related plugins][JsPlugins].
     */
    fun plugins(configurations: JsPlugins.() -> Unit) =
        plugins.run(configurations)

}
