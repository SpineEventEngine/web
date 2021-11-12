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
import org.apache.tools.ant.taskdefs.condition.Os

/**
 * Information about JavaScript environment.
 *
 * Describes used JavaScript-specific tools and their input and/or output files.
 */
interface JsEnvironment {

    /**
     * A directory from which JavaScript tools are to be run.
     */
    val workingDir: File

    /**
     * Command to run `NPM` package manager.
     *
     * Default value is `nmp.cmd` for Windows, and `npm` for other OSs.
     */
    val nmpExecutable: String
        get() = if(isWindows()) "npm.cmd" else "npm"

    /**
     * An access token that allows installation and/or publishing modules.
     *
     * Default value is read from the environmental variable - `NPM_TOKEN` (PATH variable).
     * "PUBLISHING_FORBIDDEN" stub value would be assigned in case `NPM_TOKEN` variable is not set.
     *
     * See [Creating and viewing access tokens | npm Docs](https://docs.npmjs.com/creating-and-viewing-access-tokens)
     */
    val npmAuthToken: String
        get() = System.getenv("NPM_TOKEN") ?: "PUBLISHING_FORBIDDEN"

    /**
     * Path to `node_modules` directory.
     *
     * Default value is `$workingDir/node_modules`.
     */
    val nodeModulesDir: String
        get() = "$workingDir/node_modules"

    /**
     * Path to`package.json` file.
     *
     * Default value is `$workingDir/package.json`.
     */
    val packageJsonFile: String
        get() = "$workingDir/package.json"
}

/**
 * Configurable [JsEnvironment].
 */
class ConfigurableJsEnvironment(initialEnvironment: JsEnvironment) : JsEnvironment {

    override var workingDir = initialEnvironment.workingDir
    override var nmpExecutable = initialEnvironment.nmpExecutable
    override var npmAuthToken = initialEnvironment.npmAuthToken
    override var nodeModulesDir = initialEnvironment.nodeModulesDir
    override var packageJsonFile = initialEnvironment.packageJsonFile
}

private fun isWindows(): Boolean = Os.isFamily(Os.FAMILY_WINDOWS)
