/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.io.Files
import com.google.common.collect.Lists


apply(from = "$rootDir/scripts/js.gradle")

val spineCoreVersion: String by extra

dependencies {
    protobuf(project(":web"))
    protobuf(group = "io.spine", name = "spine-client", version = spineCoreVersion, classifier = "proto")
    testProtobuf(group = "io.spine", name = "spine-client", version = spineCoreVersion, classifier = "proto")
}

idea.module {
    sourceDirs.add(file(project.extra["srcDir"]))
    testSourceDirs.add(file(project.extra["testSrcDir"]))

    excludeDirs.addAll(files(
            project.extra["nycOutputDir"],
            project.extra["genProtoMain"],
            project.extra["genProtoTest"]
    ))
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

val jsDocDir = Files.createTempDir()

val jsDoc by tasks.registering(type = Exec::class) {
    commandLine("$projectDir/node_modules/.bin/jsdoc", "-r", "-d", jsDocDir.path, "$projectDir/main/")
}

afterEvaluate {
    val generatedDocs = "generatedDocs"
    val predefinedDocs = extra[generatedDocs] as Iterable<File>
    val newDocs = Lists.newArrayList(predefinedDocs)
    newDocs.add(file(jsDocDir))
    extra[generatedDocs] = newDocs
    tasks.getByName("updateGitHubPages").dependsOn(jsDoc)
}
