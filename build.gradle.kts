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

import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.build.CheckerFramework
import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Guava
import io.spine.dependency.lib.HttpClient
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.JavaX
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.gradle.javadoc.JavadocConfig
import io.spine.gradle.github.pages.updateGitHubPages
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.publish.PublishingRepos
import io.spine.gradle.publish.spinePublishing
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.gradle.report.coverage.KoverConfig
import io.spine.gradle.report.license.LicenseReporter
import io.spine.gradle.report.pom.PomGenerator
import io.spine.gradle.testing.configureLogging
import io.spine.gradle.testing.registerTestTasks

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
buildscript {
    apply(from = "$rootDir/version.gradle.kts")

    standardSpineSdkRepositories()
    doForceVersions(configurations)

    val spineMcJsVersion: String by extra

    configurations {
        all {
            exclude(group = "io.spine", module = "spine-flogger-api")
            exclude(group = "io.spine", module = "spine-logging-backend")
            resolutionStrategy {
                val jackson = io.spine.dependency.lib.Jackson
                val cfg = this@all
                val rs = this@resolutionStrategy
                jackson.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.Jackson.DataType.forceArtifacts(project, cfg, rs)

                // `spine-mc-js` (the JavaScript code generator) still depends on the
                // legacy Model Compiler tooling, whose artifacts have been removed from
                // the repositories. Redirect those references onto the currently published
                // ones so that the buildscript classpath can be resolved.
                //
                // The `spine-tool-base` artifact was renamed to `tool-base`, and its
                // `2.0.0-SNAPSHOT.*` line is no longer published. The `spine-plugin-base`
                // version required transitively (`.192`) is also gone, so we pin it to the
                // latest available (`.130`).
                dependencySubstitution {
                    substitute(module("io.spine.tools:spine-tool-base"))
                        .using(module(io.spine.dependency.local.ToolBase.lib))
                }

                // The legacy `spine-mc-js` dependencies bring in older Kotlin artifacts
                // (e.g., `kotlin-stdlib-jdk8:1.8.22`). Their versions cannot be aligned via
                // the Kotlin BOM alone, so pin every Kotlin artifact to the version used by
                // `config`.
                eachDependency {
                    if (requested.group == io.spine.dependency.lib.Kotlin.group) {
                        useVersion(io.spine.dependency.lib.Kotlin.runtimeVersion)
                    }
                }

                val logging = io.spine.dependency.local.Logging
                force(
                    jackson.annotations,
                    jackson.bom,
                    io.spine.dependency.lib.Guava.lib,
                    io.spine.dependency.lib.Kotlin.bom,
                    io.spine.dependency.local.Base.annotations,
                    io.spine.dependency.local.Base.lib,
                    io.spine.dependency.local.Base.environment,
                    io.spine.dependency.local.Base.format,
                    io.spine.dependency.local.Time.lib,
                    io.spine.dependency.local.Time.javaExtensions,
                    io.spine.dependency.local.Compiler.api,
                    io.spine.dependency.local.Compiler.pluginLib,
                    io.spine.dependency.local.Compiler.gradleApi,
                    io.spine.dependency.local.Compiler.params,
                    io.spine.dependency.local.ToolBase.lib,
                    io.spine.dependency.local.CoreJvm.server,
                    io.spine.dependency.local.Reflect.lib,
                    logging.lib,
                    logging.libJvm,
                    logging.grpcContext,
                    io.spine.dependency.local.Validation.runtime,
                    io.spine.dependency.build.OsDetector.lib,
                    "io.spine.tools:spine-plugin-base:2.0.0-SNAPSHOT.130",
                )
            }
        }
    }

    dependencies {
        classpath(enforcedPlatform(io.spine.dependency.lib.Grpc.bom))
        classpath(enforcedPlatform(io.spine.dependency.kotlinx.Coroutines.bom))
        classpath(io.spine.dependency.local.CoreJvmCompiler.pluginLib)
        classpath("io.spine.tools:spine-mc-js:$spineMcJsVersion")
    }
}

repositories.standardToSpineSdk()

plugins {
    `java-library`
    kotlin("jvm")
    idea
    `project-report`
    protobuf
    errorprone
}

spinePublishing {
    modules = setOf(
        "web",
        "firebase-web",
        "testutil-web"
    )
    destinations = with(PublishingRepos) {
        setOf(
            gitHub("web"),
            cloudArtifactRegistry
        )
    }
}

allprojects {
    apply {
        plugin("idea")
        plugin("project-report")

        from("$rootDir/version.gradle.kts")
    }

    group = "io.spine"
    version = extra["versionToPublish"]!!
}

subprojects {

    apply {
        plugin("java-library")
        plugin("kotlin")
        plugin("com.google.protobuf")
        plugin("net.ltgt.errorprone")
        plugin("maven-publish")

        // Apply custom Kotlin script plugins.
        plugin("pmd-settings")
        plugin("dokka-setup")
    }
    apply<BomsPlugin>()

    repositories.standardToSpineSdk()

    dependencies {
        ErrorProne.apply {
            errorprone(core)
        }

        Protobuf.libs.forEach { api(it) }
        api(Guava.lib)
        api(CheckerFramework.annotations)
        api(JavaX.annotations)
        ErrorProne.annotations.forEach { api(it) }
        api(kotlin("stdlib-jdk8"))

        testImplementation(enforcedPlatform(JUnit.bom))
        testImplementation(JUnit.Jupiter.api)
        testImplementation(JUnit.Jupiter.params)
        testRuntimeOnly(JUnit.Jupiter.engine)
        testRuntimeOnly(JUnit.Platform.launcher)
        testImplementation(TestLib.lib)
        testImplementation(CoreJvm.clientTestLib)
    }

    configurations {
        forceVersions()
        all {
            resolutionStrategy {
                // Resolve version conflicts arising from this project's transitive
                // dependencies (Google HTTP Client, gRPC, Firebase, the Spine
                // libraries, and the Jackson stack pulled in by the Spine Compiler
                // at slightly different versions).
                val cfg = this@all
                val rs = this@resolutionStrategy
                Jackson.forceArtifacts(project, cfg, rs)
                Jackson.DataType.forceArtifacts(project, cfg, rs)
                Jackson.DataFormat.forceArtifacts(project, cfg, rs)
                force(
                    Jackson.annotations,
                    Jackson.bom,
                    Kotlin.bom,
                    JUnit.bom,
                    Base.lib,
                    Base.annotations,
                    Base.environment,
                    Base.format,
                    Time.lib,
                    Time.javaExtensions,
                    Compiler.api,
                    Compiler.pluginLib,
                    Compiler.gradleApi,
                    Compiler.params,
                    ToolBase.lib,
                    CoreJvm.server,
                    Reflect.lib,
                    Logging.lib,
                    Logging.libJvm,
                    Logging.grpcContext,
                    Validation.runtime,
                    HttpClient.google,
                    "org.apache.httpcomponents:httpclient:4.5.14",
                    Grpc.bom,
                    "${Grpc.api}:${Grpc.version}",
                    "${Grpc.core}:${Grpc.version}",
                    "${Grpc.context}:${Grpc.version}",
                    "${Grpc.stub}:${Grpc.version}",
                )
            }
        }
    }

    java {
        sourceCompatibility = BuildSettings.javaVersionCompat
        targetCompatibility = BuildSettings.javaVersionCompat

        tasks.withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
        }
    }

    kotlin {
        explicitApi()

        compilerOptions {
            jvmTarget.set(BuildSettings.jvmTarget)
            freeCompilerArgs.add("-Xskip-prerelease-check")
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

    updateGitHubPages {
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

KoverConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)
