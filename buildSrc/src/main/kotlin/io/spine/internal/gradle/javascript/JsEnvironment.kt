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

import java.io.File
import org.apache.tools.ant.taskdefs.condition.Os

/**
 * Information about JavaScript environment.
 *
 * Consists of three parts describing:
 *
 *  1. A module itself;
 *  2. Tools and their input/output files;
 *  3. Code generation.
 */
interface JsEnvironment {


    // A module itself.


    /**
     * Module's root catalog.
     */
    val projectDir: File

    /**
     * Module's version.
     */
    val moduleVersion: String

    /**
     * Module's production sources directory.
     *
     * Default value: "projectDir/main".
     */
    val srcDir: File
        get() = projectDir.resolve("main")

    /**
     * Module's test sources directory.
     *
     * Default value: "projectDir/test".
     */
    val testSrcDir: File
        get() = projectDir.resolve("test")

    /**
     * A directory which all artifacts are generated into.
     *
     * Default value: "projectDir/build".
     */
    val buildDir: File
        get() = projectDir.resolve("build")

    /**
     * A directory where artifacts for further publishing would be prepared.
     *
     * Default value: "buildDir/npm-publication".
     */
    val publicationDir: File
        get() = buildDir.resolve("npm-publication")


    // Tools and their input/output files.


    /**
     * Executable's file name to run `npm` package manager.
     *
     * Default value:
     *
     *  1. "nmp.cmd" for Windows;
     *  2. "npm" for other OSs.
     */
    val npmExecutable: String
        get() = if (isWindows()) "npm.cmd" else "npm"

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
     * A directory where `npm` puts downloaded module's dependencies.
     *
     * Default value: "projectDir/node_modules".
     */
    val nodeModules: File
        get() = projectDir.resolve("node_modules")

    /**
     * Module's descriptor used by `npm`.
     *
     * Default value: "projectDir/package.json".
     */
    val packageJson: File
        get() = projectDir.resolve("package.json")

    /**
     * `npm` gets its configuration settings from the command line, environment variables,
     * and `npmrc` files.
     *
     * Default value: "projectDir/.npmrc".
     *
     * See [npmrc | npm Docs](https://docs.npmjs.com/cli/v8/configuring-npm/npmrc)
     */
    val npmrcFile: File
        get() = projectDir.resolve(".npmrc")

    /**
     * A cache directory in which `nyc` tool outputs raw coverage report.
     *
     * See [istanbuljs/nyc](https://github.com/istanbuljs/nyc)
     */
    val nycOutput: File
        get() = projectDir.resolve(".nyc_output")

    /**
     * A directory in which `webpack` would put a ready-to-use bundle.
     *
     * See [webpack - npm](https://www.npmjs.com/package/webpack)
     */
    val webPackOutput: File
        get() = projectDir.resolve("dist")

    /**
     * A directory where bundled artifacts for further publishing would be prepared.
     */
    val webPackPublicationDir: File
        get() = publicationDir.resolve("dist")


    // Code generation.


    /**
     * Name of a directory that contains generated code.
     */
    val genProtoDirName: String
        get() = "proto"

    /**
     * Directory with production Protobuf messages compiled into JavaScript.
     */
    val genProtoMain: File
        get() = srcDir.resolve(genProtoDirName)

    /**
     * Directory with test Protobuf messages compiled into JavaScript.
     */
    val genProtoTest: File
        get() = testSrcDir.resolve(genProtoDirName)
}

/**
 * Allows overriding [JsEnvironment]'s defaults.
 *
 * All of defined properties can be split into two groups:
 *
 *  1. The ones that *define* something - can be overridden;
 *  2. The ones that *describe* something - can not be overridden.
 *
 *  Overriding of "describing" properties leads to inconsistency with expectations. They are not
 *  to be utilized by `NPM` or any other js tool. But rather by tasks that use that tool.
 *
 *  Therefore, the next properties could not be overridden:
 *
 *  1. [JsEnvironment.nodeModules];
 *  2. [JsEnvironment.packageJson];
 *  3. [JsEnvironment.npmrcFile];
 *  4. [JsEnvironment.nycOutput].
 */
class ConfigurableJsEnvironment(initialEnvironment: JsEnvironment)
    : JsEnvironment by initialEnvironment
{
    // A module itself.

    override var projectDir = initialEnvironment.projectDir
    override var moduleVersion = initialEnvironment.moduleVersion
    override var srcDir = initialEnvironment.srcDir
    override var testSrcDir = initialEnvironment.testSrcDir
    override var buildDir = initialEnvironment.buildDir
    override var publicationDir = initialEnvironment.publicationDir

    // Tools and their input/output files.

    override var npmExecutable = initialEnvironment.npmExecutable
    override var npmAuthToken = initialEnvironment.npmAuthToken
    override var webPackOutput = initialEnvironment.webPackOutput
    override var webPackPublicationDir = initialEnvironment.webPackPublicationDir

    // Code generation.

    override var genProtoDirName = initialEnvironment.genProtoDirName
    override var genProtoMain = initialEnvironment.genProtoMain
    override var genProtoTest = initialEnvironment.genProtoTest
}

internal fun isWindows(): Boolean = Os.isFamily(Os.FAMILY_WINDOWS)
