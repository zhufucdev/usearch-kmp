plugins {
    `kotlin-multiplatform`
    `android-library`
    id("org.xbib.gradle.plugin.cmake")
}

cmake {
    sourceFolder = file("$projectDir/src/cppMain")
}

