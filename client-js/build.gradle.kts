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
import io.spine.internal.gradle.fs.LazyTempPath
import io.spine.internal.gradle.js.js
import io.spine.internal.gradle.js.task.build

apply(from = "$rootDir" + io.spine.internal.gradle.Scripts.commonPath + "js/js.gradle")

plugins {
    // print task's tree
    // used for developing aims only
    id("com.dorongold.task-tree") version "2.1.0"
}

/*

spine {
    useJava()
    useDart()
    useJs()
}

spine {
    java()
    dart()
    js()
}

 */

js {
    environment {
    }
    tasks {
        register {
            val versionToPublishJs: String by extra
            build(versionToPublishJs)
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
