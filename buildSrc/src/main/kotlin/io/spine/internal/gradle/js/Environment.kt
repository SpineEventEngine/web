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
 * Information about JavaScript-specific tools and their input and/or output files.
 */
interface JsEnvironment {

    /**
     * A directory from which JavaScript tools are to be run.
     */
    val projectDir: File

    val srcDir: File
        get() = projectDir.resolve("main")

    val testSrcDir: File
        get() = projectDir.resolve("test")

    /**
     * The build directory is the directory which all artifacts are generated into.
     *
     * Default value: "projectDir/build".
     */
    val buildDir: File
        get() = projectDir.resolve("build")

    /**
     * Version to be specified in [packageJsonFile].
     */
    val moduleVersion: String

    /**
     * Path to a directory where artifacts for publishing would be prepared.
     *
     * Default value: "buildDir/npm-publication".
     */
    val publicationDirectory: File
        get() = buildDir.resolve("npm-publication")


    // ******************************************************


    /**
     * Command to run `npm` package manager.
     *
     * Default value:
     *
     *  1. "nmp.cmd" for Windows;
     *  2. "npm" for other OSs.
     */
    val nmpExecutable: String
        get() = if (isWindows()) "npm.cmd" else "npm"

    val coverageScript: String
        get() = if(isWindows()) "coverage:win" else "coverage:unix"

    /**
     * An access token that allows installation and/or publishing modules.
     *
     * Default value is read from the environmental variable - `NPM_TOKEN`.
     * "PUBLISHING_FORBIDDEN" stub value would be assigned in case `NPM_TOKEN` variable is not set.
     *
     * See [Creating and viewing access tokens | npm Docs](https://docs.npmjs.com/creating-and-viewing-access-tokens)
     */
    val npmAuthToken: String
        get() = System.getenv("NPM_TOKEN") ?: "PUBLISHING_FORBIDDEN"

    /**
     * node_modules` directory.
     *
     * Default value: "projectDir/node_modules".
     */
    val nodeModulesDir: File
        get() = projectDir.resolve("node_modules")

    /**
     * Path to `package.json` file.
     *
     * Default value: "workingDir/package.json".
     */
    val packageJsonFile: String
        get() = "$projectDir/package.json"

    val npmrcFile: File
        get() = projectDir.resolve(".npmrc")


    // ******************************************************


    /**
     * Directory with production Protobuf messages compiled into JavaScript.
     */
    val genProtoMain: File
        get() = projectDir
            .resolve("main")
            .resolve(genProtoSubDirName)

    /**
     * Directory with test Protobuf messages compiled into JavaScript.
     */
    val genProtoTest: File
        get() = projectDir
            .resolve("test")
            .resolve(genProtoSubDirName)

    val genProtoSubDirName: String
        get() = "proto"

    val nycOutputDir: File
        get() = projectDir.resolve(".nyc_output")

    val webPackOutput: File
        get() = projectDir.resolve("dist")

    val webPackPublicationDir: File
        get() = publicationDirectory.resolve("dist")
}

/**
 * Configurable [JsEnvironment].
 *
 * Allows overriding of default values for [JsEnvironment]'s properties.
 *
 * Please note, some properties could not be overridden:
 *
 *  1. [JsEnvironment.nodeModulesDir];
 *  2. [JsEnvironment.packageJsonFile].
 *
 *  Overriding of those properties leads to inconsistency with expectations. They are not
 *  to be utilized by `NPM` but rather by tasks that clean up after `nmp`. In case of overriding
 *  `npm` would continue to use files described in the interface, while cleaning tasks
 *  would start cleaning up `nothingness`.
 *
 */
class ConfigurableJsEnvironment(initialEnvironment: JsEnvironment) : JsEnvironment {

    override var projectDir = initialEnvironment.projectDir
    override var buildDir = initialEnvironment.buildDir
    override var moduleVersion = initialEnvironment.moduleVersion
    override var nmpExecutable = initialEnvironment.nmpExecutable
    override var npmAuthToken = initialEnvironment.npmAuthToken

    // Forbidden to override

    override val nodeModulesDir = initialEnvironment.nodeModulesDir
    override val packageJsonFile = initialEnvironment.packageJsonFile
}

private fun isWindows(): Boolean = Os.isFamily(Os.FAMILY_WINDOWS)
