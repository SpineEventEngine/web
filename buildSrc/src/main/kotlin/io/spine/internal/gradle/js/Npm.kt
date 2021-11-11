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
 * Access point to a local installation of `NPM` package manager.
 */
object Npm {

    /**
     * Name of an environmental variable (PATH variable) which contains the NPM auth token.
     */
    private const val tokenVariable = "NPM_TOKEN"

    /**
     * Reads value of [tokenVariable] from the current environment. Returns "PUBLISHING_FORBIDDEN"
     * stub if the value is not present.
     */
    private fun readAuthToken(): String =
        System.getenv(tokenVariable) ?: "PUBLISHING_FORBIDDEN"

    /**
     * Executes the given command depending on the current OS.
     *
     * @param workingDir the directory to execute the command in
     * @param windowsCommand the command to execute if OS is Windows
     * @param unixCommand the command to execute if OS is Unix-like
     * @param params the command params, platform-independent
     */
    private fun execMultiplatform(
        workingDir: File,
        windowsCommand: String,
        unixCommand: String,
        params: List<String>
    ) {

        val command = if (Os.isFamily(Os.FAMILY_WINDOWS)) windowsCommand else unixCommand
        val resultingParams = command + params
    }
}
