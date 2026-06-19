/*
 * Copyright 2022, TeamDev. All rights reserved.
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
import io.spine.dependency.lib.Firebase
import io.spine.dependency.lib.HttpClient
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.lib.Slf4J
import io.spine.dependency.local.BaseTypes
import io.spine.gradle.checkstyle.CheckStyleConfig

plugins {
    id("io.spine.core-jvm")
}

group = "io.spine.gcloud"

configurations.excludeProtobufLite()

// The Firebase Admin SDK pulls a large Google Cloud dependency tree with internally
// conflicting versions (e.g., Firestore `3.38.0` vs `3.26.5`). Enforcing the Google Cloud
// BOM that `firebase-admin:9.9.0` itself imports aligns the whole tree. Applied to every
// resolvable configuration so that the Protobuf compilation classpaths are covered too.
run {
    val boms = listOf(
        "com.google.cloud:libraries-bom:26.77.0",
        // The Google Cloud BOM pins the Cloud libraries to versions that expect
        // OpenTelemetry `1.51.0`, but does not manage all OpenTelemetry SDK artifacts.
        "io.opentelemetry:opentelemetry-bom:1.51.0",
    )
    // Apply the BOMs only to the dependency-bucket configurations (not to tool-specific
    // ones such as the Kotlin build-tools classpath, whose versions are managed elsewhere).
    val targetConfigurations = setOf(
        "api", "implementation", "compileOnly", "runtimeOnly",
        "testImplementation", "testCompileOnly", "testRuntimeOnly",
        "protobuf", "testProtobuf",
    )
    configurations.matching { it.name in targetConfigurations }.configureEach {
        withDependencies {
            boms.forEach { add(project.dependencies.enforcedPlatform(it)) }
        }
    }
}

dependencies {
    api(BaseTypes.lib)
    api(project(":web"))
    api(Firebase.admin) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "io.grpc")
    }

    implementation(Jackson.databind)
    implementation(HttpClient.apache)

    // Required by the Firebase Admin SDK.
    @Suppress("DEPRECATION", "RemoveRedundantQualifierName")
    runtimeOnly(Slf4J.lib)

    testImplementation(project(":testutil-web"))
}

val compileProtoToJs by tasks.registering {
    description = "Compiles Protobuf sources into JavaScript."
}

CheckStyleConfig.applyTo(project)

protobuf {
    protoc {
        artifact = Protobuf.compiler
    }
    // TODO: Restore Protobuf-to-JavaScript generation once the JS toolchain is
    //  compatible with the new `config`. The `js` output is no longer a `protoc`
    //  built-in (it requires the external `protoc-gen-js` plugin), and `spine-mc-js`
    //  is not published against the current Spine artifacts. See `client-js` and the
    //  commented-out JS modules in `settings.gradle.kts`.
    generateProtoTasks {
        all().forEach { task ->
            compileProtoToJs {
                dependsOn(task)
            }
        }
    }
}
