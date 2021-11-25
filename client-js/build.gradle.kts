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
import io.spine.internal.gradle.javascript.javascript
import io.spine.internal.gradle.javascript.plugins.idea
import io.spine.internal.gradle.javascript.plugins.mcJs
import io.spine.internal.gradle.javascript.plugins.protobuf
import io.spine.internal.gradle.javascript.task.impl.assemble
import io.spine.internal.gradle.javascript.task.impl.check
import io.spine.internal.gradle.javascript.task.impl.clean
import io.spine.internal.gradle.javascript.task.impl.publish
import io.spine.internal.gradle.javascript.task.impl.webPack

javascript {
    tasks {
        register {
            assemble()
            publish()
            clean()
            check()
        }
        configure {
            webPack()
        }
    }
    plugins {
        mcJs()
        protobuf()
        idea()
    }
}

dependencies {

    val spineCoreVersion: String by project.extra


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

tasks {

    // Suppress building the JS project as a Java module.

    compileJava.configure {
        enabled = false
    }
    compileTestJava.configure {
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
