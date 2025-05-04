@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `maven-publish`
    bridge
    cmake
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublisher)
}

cmake {
    sourceFolder = file("$projectDir/src/cppMain")
}

val headerGenerator = tasks.create("generateJniHeaders", JniHeaderTask::class) {
    classpath = files("src/jvmCommonMain/bridge")
    source = fileTree(classpath.asPath) {
        include("**/*.java")
    }
    headerOutputDir.value(cmake.sourceFolder.dir("bridging"))
}


android {
    namespace = Library.namespace
    compileSdk = 35

    defaultConfig {
        minSdk = 27
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("jvmCommon") {
                withJvm()
                withAndroidTarget()
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions.jvmTarget = "11"
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    listOf(
        linuxX64(),
        linuxArm64(),
        mingwX64(),
        macosX64(),
        macosArm64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.cmakeNative()
        it.binaries {
            sharedLib()
            staticLib()
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        allWarningsAsErrors = true
    }

    sourceSets {
        val commonMain by getting { }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val jvmMain by getting {

        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
        val jvmCommonMain by getting {
            kotlin {
                srcDir(headerGenerator.classpath.singleFile)
            }

            dependencies {
                implementation(files(headerGenerator.destinationDirectory))
            }
        }
        val nativeMain by getting {
        }
        val nativeTest by getting {
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.bundles.androidx.test)
            }
        }
    }
}

val androidSetup = cmakeAndroid()

androidComponents {
    onVariants {
        afterEvaluate {
            tasks.withType<com.android.build.gradle.internal.tasks.BaseTask> {
                dependsOn(*androidSetup.jniTasks.toTypedArray())
            }
        }
    }
}

android {
    sourceSets.configureEach {
        jniLibs.srcDir(androidSetup.jniDirectory)
    }
}

cmakeJvm()

afterEvaluate {
    tasks.withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release")
        )
    )

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(Library.namespace, "core", "0.2.4")
    pom {
        name = "USearch KMP"
        description = "Kotlin Multiplatform binding for USearch."
        url = "https://github.com/zhufucdev/usearch-kmp"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                name = "Steve Reed"
                email = "zhufuzhufu1@gmail.com"
                id = "zhufucdev"
            }
        }
        scm {
            url = "https://github.com/zhufucdev/usearch-kmp"
            connection = "scm:git:git://github.com/zhufucdev/usearch-kmp.git"
            developerConnection = "scm:git:ssh://github.com/zhufucdev/usearch-kmp.git"
        }
    }
}
