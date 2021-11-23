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

package io.spine.internal.gradle.js

import java.io.File
import org.gradle.api.Project

/**
 * Context for facilitating setting up of JavaScript-related tasks and plugins.
 *
 * The context provides:
 *
 *  1. Access to the current [JsEnvironment];
 *  2. API for running `nmp` command.
 */
open class JsContext(jsEnv: JsEnvironment, private val project: Project)
    : JsEnvironment by jsEnv
{

    /**
     * Runs an `npm` command in a separate process.
     *
     * The current [Project.getProjectDir] is used as a working directory.
     *
     * Please note, this method is supposed to be called during tasks execution.
     *
     * Usage example:
     *
     * ```
     * fun JsTaskRegistering.customTask() = register("customTask") {
     *     doLast {
     *         npm("set", "audit", "false")
     *         npm("install")
     *     }
     * }
     * ```
     */
    fun npm(vararg args: String) = projectDir.npm(*args)

    /**
     * Runs an `npm` command in a separate process using this [File] as a working directory.
     *
     * Please note, this method is supposed to be called during tasks execution.
     *
     * Usage example:
     *
     * ```
     * fun JsTaskRegistering.customTask() = register("customTask") {
     *     doLast {
     *         val workingDir = File("path_to_specific_working_dir")
     *         workingDir.npm("set", "audit", "false")
     *         workingDir.npm("install")
     *     }
     * }
     * ```
     */
    fun File.npm(vararg args: String) = project.exec {

        workingDir(this@npm)
        commandLine(npmExecutable)
        args(*args)

        // Using private packages in a CI/CD workflow | npm Docs
        // https://docs.npmjs.com/using-private-packages-in-a-ci-cd-workflow

        environment["NPM_TOKEN"] = npmAuthToken
    }
}
