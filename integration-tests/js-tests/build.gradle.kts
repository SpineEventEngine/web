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

import com.google.protobuf.gradle.*
import groovy.lang.Closure
import io.spine.internal.gradle.js.javascript
import io.spine.internal.gradle.js.task.impl.build

plugins {
    id("io.spine.mc-js")
}

//apply(from = "$rootDir" + io.spine.internal.gradle.Scripts.commonPath + "js/build-tasks.gradle")

javascript {
    tasks {
        register {
            build()
        }
    }
}

val testSrcDir: String = "$projectDir/test"
val genProtoBaseDir: String = projectDir.path
val genProtoSubDir: String = "proto"
val genProtoTestDir: String = "$testSrcDir/$genProtoSubDir"
val nycOutputDir: String = "$projectDir/.nyc_output"

dependencies {
    testProtobuf(project(":test-app")) {
        exclude(group = "com.google.firebase")
    }
}

/**
 * Cleans old module dependencies and build outputs.
 */
tasks.register(name = "deleteCompiled", type = Delete::class ) {
    description = "Cleans old module dependencies and build outputs."
    delete(genProtoTestDir, nycOutputDir)
    tasks.clean.get().dependsOn(this)
}

val npm: Closure<*> by extra

/**
 * Installs unpublished artifact of `spine-web` library as a module dependency.
 *
 * Creates a symbolic link from globally-installed `spine-web` library to `node_modules` of
 * the current project. See https://docs.npmjs.com/cli/link for details.
 */
tasks.register("installLinkedLib") {
    description = "Install unpublished artifact of `spine-web` library as a module dependency."

    dependsOn(":client-js:publishJsLocally")

    doLast {
        npm.call("run", "installLinkedLib")
    }
}

// TODO:2019-05-29:yegor.udovchenko: Find a way to run the same tests against `spine-web`
// source code in `client-js` module to recover coverage.
// See https://github.com/SpineEventEngine/web/issues/96
/**
 * Runs integration tests of the `spine-web` library against the sample Spine-based application.
 *
 * Runs the sample Spine-based application from the `test-app` module before integration
 * tests and stops it when tests complete. See `./integration-tests/README.MD` for details.
 */
tasks.register("integrationTest") {
    description = "Runs integration tests of the `spine-web` library against the sample application."

    dependsOn("build", "installLinkedLib", ":test-app:appBeforeIntegrationTest")
    finalizedBy(":test-app:appAfterIntegrationTest")

    doLast {
        npm.call("run", "test")
    }
}

protoJs {
    generatedTestDir = genProtoTestDir

    generateParsersTask().dependsOn("compileProtoToJs")
    tasks["buildJs"].dependsOn(generateParsersTask())
}

protobuf {
    generatedFilesBaseDir = genProtoBaseDir
    protoc {
        artifact = io.spine.internal.dependency.Protobuf.compiler
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // Do not use java builtin output in this project.
                remove("java")

                // For information on JavaScript code generation please see
                // https://github.com/google/protobuf/blob/master/js/README.md
                id("js") {
                    option("import_style=commonjs")
                    outputSubDir = genProtoSubDir
                }

                task.generateDescriptorSet = true
                val testClassifier = if (task.sourceSet.name == "test") "_test" else ""
                val descriptorName = "${project.group}_${project.name}_${project.version}${testClassifier}.desc"
                task.descriptorSetOptions.path = "${projectDir}/build/descriptors/${task.sourceSet.name}/${descriptorName}"
            }
            tasks["compileProtoToJs"].dependsOn(task)
        }
    }
}

idea.module {
    testSourceDirs.add(file(testSrcDir))
    excludeDirs.add(file(genProtoTestDir))
}

// Suppress building the JS project as a Java module.
tasks.compileJava {
    enabled = false
}
tasks.compileTestJava {
    enabled = false
}

// Suppress audit for a test project.
tasks["auditNodePackages"].enabled = false
