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

import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.testProtobuf
import io.spine.internal.gradle.js.task.buildJs
import io.spine.internal.gradle.js.task.testJs
import io.spine.internal.gradle.fs.LazyTempPath
import io.spine.internal.gradle.js.configureProtobuf
import io.spine.internal.gradle.js.javascript
import io.spine.internal.gradle.js.task.impl.build
import io.spine.internal.gradle.js.task.impl.other
import io.spine.internal.gradle.js.task.impl.publish
import io.spine.internal.gradle.js.task.impl.webPack

javascript {
    tasks {
        register {
            build()
            publish()
            other()
        }
        configure {
            webPack()
            publish()
        }
    }

    configureProtobuf()
}

apply(from = "$rootDir" + io.spine.internal.gradle.Scripts.commonPath + "js/js.gradle")

tasks {

    // For migration aims.

    register("listPlugins") {
        doLast {
            println("Applied plugins: ${project.plugins.size}")
            project.plugins.forEach(::println)
        }
    }

    register("checkProtoJs") {
        doLast {
            project.extensions.configure<io.spine.tools.mc.js.gradle.McJsExtension>("protoJs") {

                println("generatedMainDir: $generatedMainDir")
                println("generatedTestDir: $generatedTestDir")

                println(generateParsersTask().taskDependencies.getDependencies(generateParsersTask()).sorted())
                println(buildJs.taskDependencies.getDependencies(buildJs).sorted())
                println(testJs.taskDependencies.getDependencies(testJs).sorted())
            }
        }
    }
}

val spineCoreVersion: String by extra

dependencies {
    protobuf(project(":web"))
    protobuf(project(":firebase-web"))
    protobuf(
        group = "io.spine",
        name = "spine-client",
        version = spineCoreVersion,
        classifier = "proto"
    )
    testProtobuf(
        group = "io.spine",
        name = "spine-client",
        version = spineCoreVersion,
        classifier = "proto"
    )
}

idea.module {
    sourceDirs.add(file(project.extra["srcDir"]!!))
    testSourceDirs.add(file(project.extra["testSrcDir"]!!))

    excludeDirs.addAll(
        files(
            project.extra["nycOutputDir"],
            project.extra["genProtoMain"],
            project.extra["genProtoTest"]
        )
    )
}


sourceSets {
    main {
        java.exclude("**/*.*")
        resources.exclude("**/*.*")
    }
    test {
        java.exclude("**/*.*")
        resources.exclude("**/*.*")
    }
}

// Suppress building the JS project as a Java module.
tasks.compileJava {
    enabled = false
}

tasks.compileTestJava {
    enabled = false
}

val jsDocDir = LazyTempPath("jsDocs")

val jsDoc by tasks.registering(type = Exec::class) {
    commandLine(
        "$projectDir/node_modules/.bin/jsdoc",
        "-r",
        "-d",
        jsDocDir,
        "$projectDir/main/"
    )
}

afterEvaluate {
    updateGitHubPages {
        includeInputs.add(jsDocDir)
    }
    tasks.getByName("updateGitHubPages").dependsOn(jsDoc)
}
