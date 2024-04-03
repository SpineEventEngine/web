/*
 * Copyright 2024, TeamDev. All rights reserved.
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
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.base.check
import io.spine.internal.gradle.fs.LazyTempPath
import io.spine.internal.gradle.typescript.plugin.idea
import io.spine.internal.gradle.typescript.plugin.protobuf
import io.spine.internal.gradle.typescript.task.assemble
import io.spine.internal.gradle.typescript.task.clean
import io.spine.internal.gradle.typescript.task.check
import io.spine.internal.gradle.typescript.task.coverageTs
import io.spine.internal.gradle.typescript.task.publish
import io.spine.internal.gradle.typescript.task.webpack
import io.spine.internal.gradle.typescript.typescript

dependencies {

    val spineCoreVersion = Spine.CoreJava.version

    protobuf(project(":web"))

    protobuf(project(":firebase-web"))

    protobuf(
        group = "io.spine",
        name = "spine-client",
        version = spineCoreVersion,
        classifier = "proto"
    ) {
        exclude(Protobuf.protoSrcLib)
    }

    testProtobuf(
        group = "io.spine",
        name = "spine-client",
        version = spineCoreVersion,
        classifier = "proto"
    )
}

configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-kotlin")
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

typescript {
    plugins {
        protobuf()
        idea()
    }
    tasks {
        assemble {
            webpack()
        }

        clean()
        publish()

        check {
            rootProject.tasks.check.configure {
                dependsOn(coverageTs)
            }
        }

        // NPM dependencies are not included into license reports.
        // See issue: https://github.com/SpineEventEngine/config/issues/301

        // licenseReport()
    }
}

tasks {

    // Suppress building the JS project as a Java module.

    compileJava {
        enabled = false
    }
    compileTestJava {
        enabled = false
    }
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
