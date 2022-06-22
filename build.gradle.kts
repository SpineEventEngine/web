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

import io.spine.gradle.internal.DependencyResolution
import io.spine.gradle.internal.Deps
import io.spine.gradle.internal.PublishingRepos
import io.spine.gradle.internal.servletApi

buildscript {

    apply(from = "$rootDir/config/gradle/dependencies.gradle")
    apply(from = "$rootDir/version.gradle.kts")

    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    val resolution = io.spine.gradle.internal.DependencyResolution

    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    val deps = io.spine.gradle.internal.Deps

    resolution.defaultRepositories(repositories)

    val spineBaseVersion: String by extra

    dependencies {
        classpath(deps.build.guava)
        classpath(deps.build.gradlePlugins.protobuf)
        classpath(deps.build.gradlePlugins.errorProne)
        classpath("io.spine.tools:spine-model-compiler:$spineBaseVersion")
        classpath("io.spine.tools:spine-proto-js-plugin:$spineBaseVersion")
    }

    resolution.forceConfiguration(configurations)
}

plugins {
    `java-library`
    jacoco
    idea
    pmd
    `project-report`
    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    id("net.ltgt.errorprone").version(io.spine.gradle.internal.Deps.versions.errorPronePlugin)
    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    id("com.google.protobuf").version(io.spine.gradle.internal.Deps.versions.protobufPlugin)
}

extra["credentialsPropertyFile"] = PublishingRepos.cloudRepo.credentials
extra["projectsToPublish"] = listOf("web", "firebase-web", "testutil-web")

allprojects {
    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")

        from("$rootDir/version.gradle.kts")
    }

    version = extra["versionToPublish"]!!
    group = "io.spine"
}

subprojects {
    val sourcesRootDir = "$projectDir/src"
    val generatedRootDir = "$projectDir/generated"
    val generatedJavaDir = "$generatedRootDir/main/java"
    val generatedTestJavaDir = "$generatedRootDir/test/java"
    val generatedSpineDir = "$generatedRootDir/main/spine"
    val generatedTestSpineDir = "$generatedRootDir/test/spine"

    apply {
        plugin("java-library")
        plugin("com.google.protobuf")
        plugin("net.ltgt.errorprone")
        plugin("maven-publish")
        plugin("pmd")

        from(Deps.scripts.testOutput(project))
        from(Deps.scripts.javadocOptions(project))
        from(Deps.scripts.javacArgs(project))
        from(Deps.scripts.pmd(project))
        from(Deps.scripts.projectLicenseReport(project))
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    DependencyResolution.defaultRepositories(repositories)

    val spineBaseVersion: String by extra
    val spineTimeVersion: String by extra
    val spineCoreVersion: String by extra

    dependencies {
        errorprone(Deps.build.errorProneCore)
        errorproneJavac(Deps.build.errorProneJavac)

        implementation(Deps.build.guava)

        compileOnlyApi(Deps.build.checkerAnnotations)
        compileOnlyApi(Deps.build.jsr305Annotations)
        Deps.build.errorProneAnnotations.forEach { compileOnlyApi(it) }

        testImplementation("io.spine:spine-testutil-client:$spineCoreVersion")
        testImplementation(Deps.test.guavaTestlib)
        Deps.test.junit5Api.forEach { testImplementation(it) }
        Deps.test.truth.forEach { testImplementation(it) }
        testRuntimeOnly(Deps.test.junit5Runner)
    }

    DependencyResolution.forceConfiguration(configurations)

    configurations.all {
        resolutionStrategy {

            /**
             * Force transitive dependencies.
             * Common 3rd party dependencies are forced by {@code forceConfiguration()} calls above.
             *
             * The forced versions are selected as the highest among detected in the version
             * conflict. Developers <em>may</em> select a higher version as the dependency in
             * this project <em>IFF</em> this dependency is used directly or a newer version
             * fixes a security issue.
             *
             * {@code proto-google-common-protos} starting with version {@code 1.1.0}
             * and {@code proto-google-iam-v1} starting with version {@code 0.1.29}
             * include Protobuf message definitions alongside with compiled Java.
             * This breaks the Spine compiler which searches for all Protobuf definitions
             * in classpath, and assumes they implement the Type URLs.
             */
            force(
                "io.opencensus:opencensus-api:0.21.0",
                "io.opencensus:opencensus-contrib-http-util:0.18.0",

                "io.grpc:grpc-core:${Deps.versions.grpc}",
                "io.grpc:grpc-stub:${Deps.versions.grpc}",
                "io.grpc:grpc-okhttp:${Deps.versions.grpc}",
                "io.grpc:grpc-protobuf:${Deps.versions.grpc}",
                "io.grpc:grpc-netty:${Deps.versions.grpc}",
                "io.grpc:grpc-context:${Deps.versions.grpc}",
                "io.grpc:grpc-stub:${Deps.versions.grpc}",
                "io.grpc:grpc-protobuf:${Deps.versions.grpc}",
                "io.grpc:grpc-core:${Deps.versions.grpc}",

                "com.google.code.gson:gson:2.7",
                "com.google.api:api-common:1.7.0",
                "com.google.api.grpc:proto-google-common-protos:1.0.0",
                "com.google.api.grpc:proto-google-iam-v1:0.1.28",

                "com.google.cloud:google-cloud-core:1.91.3",
                "com.google.api:gax:1.49.1",

                "com.google.oauth-client:google-oauth-client:1.25.0",

                "com.google.auth:google-auth-library-credentials:0.11.0",
                "com.google.auth:google-auth-library-oauth2-http:0.11.0",

                "com.google.j2objc:j2objc-annotations:1.3",

                "com.google.http-client:google-http-client:1.29.0",
                "com.google.http-client:google-http-client-jackson2:1.29.0",

                "com.google.api-client:google-api-client:1.30.9",

                "org.apache.httpcomponents:httpclient:4.5.5",

                "com.fasterxml.jackson.core:jackson-core:2.9.9",
                "commons-collections:commons-collections:3.2.2",

                "io.netty:netty-common:4.1.34.Final",
                "io.netty:netty-buffer:4.1.34.Final",
                "io.netty:netty-transport:4.1.34.Final",
                "io.netty:netty-handler:4.1.34.Final",
                "io.netty:netty-codec-http:4.1.34.Final",

                Deps.build.servletApi,

                "org.eclipse.jetty.orbit:javax.servlet.jsp:2.2.0.v201112011158",
                "org.eclipse.jetty.toolchain:jetty-schemas:3.1",

                // Transitive dependencies from `core-java` may have different (older) versions.
                "io.spine:spine-base:$spineBaseVersion",
                "io.spine:spine-testlib:$spineBaseVersion",
                "io.spine:spine-time:$spineTimeVersion"
            )
        }
    }

    sourceSets {
        main {
            java.srcDirs(generatedJavaDir, "$sourcesRootDir/main/java", generatedSpineDir)
            resources.srcDirs("$sourcesRootDir/main/resources", "$generatedRootDir/main/resources")
        }
        test {
            java.srcDirs(generatedTestJavaDir, "$sourcesRootDir/test/java", generatedTestSpineDir)
            resources.srcDirs("$sourcesRootDir/test/resources", "$generatedRootDir/test/resources")
        }
    }

    tasks.register("sourceJar", Jar::class) {
        from(sourceSets.main.get().allJava)
        archiveClassifier.set("sources")
    }

    tasks.register("testOutputJar", Jar::class) {
        from(sourceSets.main.get().output)
        archiveClassifier.set("test")
    }

    tasks.register("javadocJar", Jar::class) {
        from("$projectDir/build/docs/javadoc")
        archiveClassifier.set("javadoc")

        dependsOn(tasks.javadoc)
    }

    tasks.test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
    }

    idea {
        module {
            generatedSourceDirs.add(file(generatedJavaDir))
            testSourceDirs.add(file(generatedTestJavaDir))
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    val projectsWithDocs = setOf("client-js", "firebase-web", "web")
    if (projectsWithDocs.contains(project.name)) {
        apply(from = Deps.scripts.updateGitHubPages(project))
        project.tasks["publish"].dependsOn("${project.path}:updateGitHubPages")
    }
}

apply {
    from(Deps.scripts.jacoco(project))
    from(Deps.scripts.publish(project))
    from(Deps.scripts.repoLicenseReport(project))
    from(Deps.scripts.generatePom(project))
}
