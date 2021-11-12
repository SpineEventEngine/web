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

package io.spine.internal.gradle.js.task

import io.spine.internal.gradle.js.JsEnvironment
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer

/**
 * Context for setting up JavaScript-related Tasks.
 *
 * The context provides:
 *
 *  1. Access to the current [JsEnvironment];
 *  2. Default task groups;
 *  3. Shortcut for running `nmp` command.
 */
open class JsTaskContext(jsEnv: JsEnvironment, private val project: Project)
    : JsEnvironment by jsEnv, TaskContainer by project.tasks
{
    // Default task groups.
    internal val jsBuildTask = "JavaScript/Build"

    /**
     * Launches a separate process which runs an `npm` command.
     *
     * Please note, this method is supposed to be run during tasks execution.
     *
     * Usage example:
     *
     * ```
     * fun JsTasks.customTask = register("customTask") {
     *     doLast {
     *         npm("set", "audit", "false")
     *         npp("install")
     *     }
     * }
     * ```
     */
    internal fun npm(vararg args: String) = project.exec {

        workingDir(workingDir)
        commandLine(nmpExecutable)
        args(args)

        // Using private packages in a CI/CD workflow | npm Docs
        // https://docs.npmjs.com/using-private-packages-in-a-ci-cd-workflow

        environment["NPM_TOKEN"] = npmAuthToken
    }
}

/**
 * ...
 */
open class JsTasks(jsEnv: JsEnvironment, project: Project)
    : JsTaskContext(jsEnv, project)
{
    private val registering = JsTaskRegistering(jsEnv, project)
    private val configuring = JsTaskConfiguring(jsEnv, project)

    /**
     * Registers new tasks.
     */
    fun register(registrations: JsTaskRegistering.() -> Unit) =
        registering.run(registrations)

    /**
     * Configures already registered tasks.
     */
    fun configure(configurations: JsTaskConfiguring.() -> Unit) =
        configuring.run(configurations)

}

/**
 * Scope for registering new tasks inside [JsTaskContext].
 */
class JsTaskRegistering(jsEnv: JsEnvironment, project: Project)
    : JsTaskContext(jsEnv, project)

/**
 * Scope for configuring present tasks inside [JsTaskContext].
 */
class JsTaskConfiguring(jsEnv: JsEnvironment, project: Project)
    : JsTaskContext(jsEnv, project)
