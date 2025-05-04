plugins {
    `kotlin-multiplatform`
    `android-library`
}

afterEvaluate {
    val jniGen = tasks.withType<JniHeaderTask>()
    tasks.withType<CMakeConfigureTask>().configureEach {
        dependsOn(*jniGen.toTypedArray())
    }
}

