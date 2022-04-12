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

import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.CommonsCodec
import io.spine.internal.dependency.CommonsCollections
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.GoogleApis
import io.spine.internal.dependency.GoogleCloud
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Gson
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.HttpClient
import io.spine.internal.dependency.HttpComponents
import io.spine.internal.dependency.J2ObjC
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.JavaX
import io.spine.internal.dependency.Jetty
import io.spine.internal.dependency.Netty
import io.spine.internal.dependency.OpenCensus
import io.spine.internal.dependency.OsDetector
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.ThreeTen
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.github.pages.updateGitHubPages
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
buildscript {
    apply(from = "$rootDir/version.gradle.kts")

    io.spine.internal.gradle.doApplyStandard(repositories)
    io.spine.internal.gradle.doApplyGitHubPackages(repositories, "base", rootProject)
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
                io.spine.internal.dependency.OsDetector.lib
            )
        }
    }
}

repositories.applyStandard()

plugins {
    `java-library`
    jacoco
    kotlin("jvm")
    idea
    pmd
    `project-report`
    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    io.spine.internal.dependency.Protobuf.GradlePlugin.apply {
        id(id)
    }
    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    io.spine.internal.dependency.ErrorProne.GradlePlugin.apply {
        id(id)
    }
}

spinePublishing {
    modules = setOf(
        "web",
        "firebase-web",
        "testutil-web"
    )
    destinations = with(PublishingRepos) {
        setOf(
            cloudRepo,
            gitHub("web"),
            cloudArtifactRegistry
        )
    }
}

allprojects {
    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")

        from("$rootDir/version.gradle.kts")
    }

    group = "io.spine"
    version = extra["versionToPublish"]!!
}

subprojects {

    val spineBaseVersion: String by extra
    val spineCoreVersion: String by extra

    apply {
        plugin("java-library")
        plugin("kotlin")
        plugin("com.google.protobuf")
        plugin("net.ltgt.errorprone")
        plugin("pmd")
        plugin("maven-publish")

        // Apply custom Kotlin script plugins.
        plugin("pmd-settings")
    }

    repositories {
        applyGitHubPackages("base", rootProject)
        applyGitHubPackages("base-types", rootProject)
        applyGitHubPackages("time", rootProject)
        applyGitHubPackages("core-java", rootProject)
        applyStandard()
    }

    dependencies {
        ErrorProne.apply {
            errorprone(core)
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
        testImplementation("io.spine.tools:spine-testutil-client:$spineCoreVersion")
    }

    configurations {
        forceVersions()
        forceTransitiveDependencies()
    }

    val javaVersion = JavaVersion.VERSION_11

    java {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion

        tasks.withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
        }
    }

    kotlin {
        explicitApi()

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions {
                jvmTarget = javaVersion.toString()
                freeCompilerArgs = listOf("-Xskip-prerelease-check")
            }
        }
    }

    tasks {
        test {
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
        }

        registerTestTasks()

        withType<Test>().configureEach {
            configureLogging()
        }
    }

    LicenseReporter.generateReportIn(project)
    JavadocConfig.applyTo(project)

    updateGitHubPages(spineBaseVersion) {
        allowInternalJavadoc.set(true)
        rootFolder.set(rootDir)
    }

    sourceSets {
        val generatedRootDir = "$projectDir/generated"
        val generatedMainDir = "$generatedRootDir/main"
        val generatedTestDir = "$generatedRootDir/test"

        main {
            java.srcDirs(
                "$generatedMainDir/spine",
                "$generatedMainDir/java"
            )
        }
        test {
            java.srcDirs(
                "$generatedTestDir/spine",
                "$generatedTestDir/java"
            )
        }
    }
}

JacocoConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)

/**
 * Force transitive dependencies.
 *
 * Common 3rd party dependencies are forced by [forceVersions].
 *
 * The forced versions are selected as the highest among detected in the version
 * conflict. Developers <em>may</em> select a higher version as the dependency in
 * this project <em>IFF</em> this dependency is used directly or a newer version
 * fixes a security issue.
 *
 * `proto-google-common-protos` starting with version `1.1.0` and `proto-google-iam-v1`
 * starting with version `0.1.29` include Protobuf message definitions alongside with compiled Java.
 * This breaks the Spine compiler which searches for all Protobuf definitions
 * in classpath, and assumes they implement the Type URLs.
 */
fun NamedDomainObjectContainer<Configuration>.forceTransitiveDependencies() = all {
    resolutionStrategy {
        val spineBaseVersion: String by extra
        val spineBaseTypesVersion: String by extra
        val spineTimeVersion: String by extra

        force(
            OpenCensus.api,
            OpenCensus.contribHttpUtil,

            Gson.lib,
            GoogleApis.common,
            GoogleApis.commonProtos,
            GoogleApis.protoAim,

            GoogleCloud.core,
            GoogleApis.gax,

            GoogleApis.oAuthClient,

            GoogleApis.AuthLibrary.credentials,
            GoogleApis.AuthLibrary.oAuth2Http,

            J2ObjC.lib,

            HttpClient.google,
            HttpClient.jackson2,
            HttpClient.gson,
            HttpClient.apache2,

            GoogleApis.client,

            ThreeTen.lib,

            HttpComponents.client,
            HttpComponents.core,

            Jackson.core,
            Jackson.databind,

            CommonsCodec.lib,
            CommonsCollections.lib,

            Netty.common,
            Netty.buffer,
            Netty.transport,
            Netty.handler,
            Netty.codecHttp,

            JavaX.servletApi,

            Jetty.orbitServletJsp,
            Jetty.toolchainSchemas,

            Flogger.lib,
            Flogger.Runtime.systemBackend,
            OsDetector.lib,

            Grpc.context,

            // Transitive dependencies from `core-java` may have different (older) versions.
            "io.spine:spine-base:$spineBaseVersion",
            "io.spine:spine-base-types:$spineBaseTypesVersion",
            "io.spine:spine-time:$spineTimeVersion",
            "io.spine.tools:spine-testlib:$spineBaseVersion"
        )
    }
}
