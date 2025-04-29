import org.xbib.gradle.plugin.cmake.CMakeConfigureTask

plugins {
    `kotlin-multiplatform`
    `android-library`
    id("org.xbib.gradle.plugin.cmake")
}

cmake {
    sourceFolder = file("$projectDir/src/cppMain")
}

afterEvaluate {
    val jniGen = tasks.withType<JniHeaderTask>()
    tasks.withType<CMakeConfigureTask>().configureEach {
        dependsOn(*jniGen.toTypedArray())
    }
}

