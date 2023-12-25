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
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.JavaX
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.github.pages.updateGitHubPages
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.standardToSpineSdk
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
buildscript {
    apply(from = "$rootDir/version.gradle.kts")

    standardSpineSdkRepositories()
    doForceVersions(configurations)
    doForceTransitiveDependencies(configurations)

    dependencies {
        classpath(io.spine.internal.dependency.Guava.lib)
        classpath(io.spine.internal.dependency.ErrorProne.GradlePlugin.lib)
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib)
        classpath(io.spine.internal.dependency.Spine.McJs.lib)
    }
}

repositories.standardToSpineSdk()

plugins {
    `java-library`
    jacoco
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
            cloudRepo,
            gitHub("web"),
            cloudArtifactRegistry
        )
    }
    dokkaJar {
        java = true
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

    configurations {
        forceVersions()
        forceTransitiveDependencies()
    }
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
        plugin("dokka-for-java")
    }

    repositories {
        standardToSpineSdk()

        applyGitHubPackages("base", rootProject)
        applyGitHubPackages("base-types", rootProject)
        applyGitHubPackages("time", rootProject)
        applyGitHubPackages("core-java", rootProject)
    }

    dependencies {
        ErrorProne.apply {
            errorprone(core)
        }

        Protobuf.libs.forEach { api(it) }
        api(Spine.Logging.lib)
        api(Guava.lib)
        api(CheckerFramework.annotations)
        api(JavaX.annotations)
        ErrorProne.annotations.forEach { api(it) }
        api(kotlin("stdlib-jdk8"))

        testImplementation(JUnit.runner)
        testImplementation(Spine.testlib)
        testImplementation(Spine.CoreJava.testUtilClient)
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

    updateGitHubPages(project.version.toString()) {
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