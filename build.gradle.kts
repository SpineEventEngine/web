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

import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.JavaX
import io.spine.internal.dependency.Protobuf
import io.spine.internal.gradle.PublishingRepos
import io.spine.internal.gradle.Scripts
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.spinePublishing
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.accessors.AccessorFormats.internal

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
buildscript {
    apply(from = "$rootDir/version.gradle.kts")

    io.spine.internal.gradle.doApplyStandard(repositories)
    io.spine.internal.gradle.doApplyGitHubPackages(repositories, rootProject)
    io.spine.internal.gradle.doForceVersions(configurations)

    val spineBaseVersion: String by extra

    dependencies {
        classpath(io.spine.internal.dependency.Guava.lib)
        classpath(io.spine.internal.dependency.ErrorProne.GradlePlugin.lib)
        classpath("io.spine.tools:spine-mc-java:$spineBaseVersion")
        classpath("io.spine.tools:spine-mc-js:$spineBaseVersion")
    }

    configurations.all {
        resolutionStrategy {
            force(
                "com.google.gradle:osdetector-gradle-plugin:1.7.0"
            )
        }
    }
}

repositories {
    repositories.applyStandard()
    repositories.applyGitHubPackages(project)
}

plugins {
    `java-library`
    jacoco
    kotlin("jvm") version io.spine.internal.dependency.Kotlin.version
    idea
    pmd
    `project-report`
    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    io.spine.internal.dependency.Protobuf.GradlePlugin.apply {
        id(id) version version
    }
    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    io.spine.internal.dependency.ErrorProne.GradlePlugin.apply {
        id(id) version version
    }
}

apply(from = "$rootDir/version.gradle.kts")

spinePublishing {
    targetRepositories.addAll(
        PublishingRepos.cloudRepo,
        PublishingRepos.gitHub("web"),
        PublishingRepos.cloudArtifactRegistry
    )

    projectsToPublish.addAll(
        "web",
        "firebase-web",
        "testutil-web"
    )
}

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
        plugin("kotlin")
        plugin("com.google.protobuf")
        plugin("net.ltgt.errorprone")
        plugin("maven-publish")
        plugin("pmd")
        plugin("pmd-settings")

        with(Scripts) {
            from(testOutput(project))
            from(javadocOptions(project))
            from(javacArgs(project))
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        explicitApi()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            freeCompilerArgs = listOf("-Xskip-prerelease-check")
        }
    }

    repositories.applyGitHubPackages(rootProject)
    repositories.applyStandard()

    val spineBaseVersion: String by extra
    val spineTimeVersion: String by extra
    val spineCoreVersion: String by extra

    dependencies {
        ErrorProne.apply {
            errorprone(core)
            errorproneJavac(javacPlugin)
        }

        Protobuf.libs.forEach { api(it) }
        api(Flogger.lib)
        api(Guava.lib)
        api(CheckerFramework.annotations)
        api(JavaX.annotations)
        ErrorProne.annotations.forEach { api(it) }
        api(kotlin("stdlib-jdk8"))

        testImplementation(JUnit.runner)
        testImplementation("io.spine.tools:spine-testlib:$spineBaseVersion")
        testImplementation("io.spine:spine-testutil-client:$spineCoreVersion")
    }

    configurations.forceVersions()
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

//                "io.grpc:grpc-core:${io.spine.gradle.internal.Deps.versions.grpc}",
//                "io.grpc:grpc-stub:${io.spine.gradle.internal.Deps.versions.grpc}",
//                "io.grpc:grpc-okhttp:${io.spine.gradle.internal.Deps.versions.grpc}",
//                "io.grpc:grpc-protobuf:${io.spine.gradle.internal.Deps.versions.grpc}",
//                "io.grpc:grpc-netty:${io.spine.gradle.internal.Deps.versions.grpc}",
//                "io.grpc:grpc-context:${io.spine.gradle.internal.Deps.versions.grpc}",
//                "io.grpc:grpc-stub:${io.spine.gradle.internal.Deps.versions.grpc}",
//                "io.grpc:grpc-protobuf:${io.spine.gradle.internal.Deps.versions.grpc}",
//                "io.grpc:grpc-core:${io.spine.gradle.internal.Deps.versions.grpc}",

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

//                io.spine.gradle.internal.Deps.build.servletApi,

                "org.eclipse.jetty.orbit:javax.servlet.jsp:2.2.0.v201112011158",
                "org.eclipse.jetty.toolchain:jetty-schemas:3.1",

                "com.google.gradle:osdetector-gradle-plugin:1.7.0",

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
        apply(from = Scripts.updateGitHubPages(project))
        project.tasks["publish"].dependsOn("${project.path}:updateGitHubPages")
    }
}

apply {
    with(Scripts) {
//        from(jacoco(project))
        //from(publish(project))
//        from(repoLicenseReport(project))
//        from(generatePom(project))
    }
}
