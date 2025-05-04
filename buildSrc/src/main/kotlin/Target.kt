import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

data class AndroidSetup(val jniTasks: List<Task>, val jniDirectory: Provider<Directory>)

fun Project.cmakeAndroid(): AndroidSetup {
    data class AndroidInstallation(
        val abi: String,
        val target: KonanTarget,
        val jniTask: CMakeBuildTask,
        val binary: Provider<FileCollection>
    )

    fun install(abi: String, target: KonanTarget): AndroidInstallation {
        val (_, _, cmakeBuildTask, binary) = install(project, target) {
            buildSharedLibs.set(true)
        }
        return AndroidInstallation(abi, target, cmakeBuildTask, binary)
    }

    val jniDir = layout.buildDirectory.dir("jniAndroid")
    val jniTasks = listOf(
        install("x86_64", KonanTarget.ANDROID_X64),
        install("arm64-v8a", KonanTarget.ANDROID_ARM64)
    ).map { install ->
        tasks.create("copy${install.target.bigCamelName}Jni", Copy::class) {
            dependsOn(install.jniTask)
            from(install.binary)
            into(jniDir.map { it.file(install.abi) })
        }
    }

    return AndroidSetup(jniTasks, jniDir)
}

fun Project.cmakeJvm() {
    val installations = listOf(
        KonanTarget.MACOS_ARM64,
        KonanTarget.MACOS_X64,
        KonanTarget.LINUX_ARM64,
        KonanTarget.LINUX_X64,
        KonanTarget.MINGW_X64
    ).map {
        install(project, it, "${it.bigCamelName}Jni") {
            buildSharedLibs.set(true)
        }
    }
    val jniDir = layout.buildDirectory.file("jniJvm")
    val copyJniResources = installations.map { installation ->
        val platformName = installation.target.bigCamelName
        tasks.create("copy${platformName}JniRes", Copy::class) {
            dependsOn(installation.cmakeBuildTask)
            from(installation.binary)
            into(jniDir)
            val target = installation.target
            rename { "${target.family.dynamicPrefix}${Library.name}${platformName}.${target.family.dynamicSuffix}" }
        }
    }

    tasks.withType<ProcessResources>().configureEach {
        dependsOn(*copyJniResources.toTypedArray())
    }

    afterEvaluate {
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.jvmMain.configure {
                resources.srcDir(jniDir)
            }
        }
    }
}

fun KotlinNativeTarget.cmakeNative(configure: CMakeConfigureForKonanTask.() -> Unit = {}) {
    val taskName = konanTarget.bigCamelName
    val (_, _, cmakeBuildTask, binary) = install(project, konanTarget) {
        buildStaticLibs.set(true)
        configure(this)
    }

    val defFileTask = project.task("generateDef$taskName", DefFileTask::class) {
        staticLinkedBinary.value(binary)
        outputFile.value(project.layout.buildDirectory.file("cinterop/${targetName}.def").map { it.asFile })

        dependsOn(cmakeBuildTask)
    }

    compilations.named("main") {
        cinterops {
            create("lib") {
                defFile(defFileTask.outputFile)
                packageName("lib")
            }
        }
    }

    project.tasks.named("cinteropLib${taskName}") {
        dependsOn(defFileTask)
    }

    project.tasks.named("compileKotlin${taskName}") {
        dependsOn(cmakeBuildTask)
    }
}

internal data class Installation(
    val target: KonanTarget,
    val cmakeConfigTask: CMakeConfigureForKonanTask,
    val cmakeBuildTask: CMakeBuildTask,
    val binary: Provider<FileCollection>
)

internal fun install(
    project: Project,
    target: KonanTarget,
    taskName: String = target.bigCamelName,
    configure: CMakeConfigureForKonanTask.() -> Unit = { }
): Installation {
    val wd = project.layout.buildDirectory.dir("cmake/${taskName.replaceFirstChar { it.lowercase() }}")

    val cmakeConfigTask = project.tasks.create("cmakeConfigure${taskName}", CMakeConfigureForKonanTask::class.java) {
        sourceFolder.set(project.file("src/cppMain"))
        workingFolder.value(wd)
        this.target.set(target)
        buildStaticLibs.set(false)
        buildSharedLibs.set(false)
        configure(this)
    }

    val cmakeBuildTask = project.tasks.create("cmakeBuild${taskName}", CMakeBuildTask::class.java) {
        configureFromProject()
        workingFolder.value(wd)
        konanTarget.set(target)
        dependsOn(cmakeConfigTask)
    }

    return Installation(
        target,
        cmakeConfigTask,
        cmakeBuildTask,
        wd.zip(cmakeConfigTask.buildSharedLibs.zip(cmakeConfigTask.buildStaticLibs) { l, r -> l to r }) { wd, (sharedLib, staticLib) ->
            wd.files(buildList {
                val family = target.family
                if (sharedLib) {
                    add("${family.dynamicPrefix}${Library.name}.${family.dynamicSuffix}")
                }
                if (staticLib) {
                    add("${family.staticPrefix}${Library.name}.${family.staticSuffix}")
                }
            }.toTypedArray())
        }
    )
}