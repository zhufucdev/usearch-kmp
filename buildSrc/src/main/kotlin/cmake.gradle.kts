import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

plugins {
    base
}

val cmakeConfigure = "cmakeConfigure"
val cmakeBuild = "cmakeBuild"

val extension = project.extensions
    .create("cmake", CMakePluginExtension::class.java, project)
val cmakeClean = project.task("cmakeClean").doFirst {
    val workingFolder = extension.workingFolder.asFile.get().getAbsoluteFile()
    if (workingFolder.exists()) {
        project.logger.info("Deleting folder $workingFolder")
        if (!workingFolder.deleteRecursively()) {
            throw GradleException("Could not delete working folder $workingFolder")
        }
    }
}
cmakeClean.group = "cmake"
cmakeClean.description = "Clean CMake configuration"
val cmakeGenerators = project.task("cmakeGenerators").doFirst {
    val pb = ProcessBuilder(
        extension.executable.getOrElse("cmake"),
        "--help"
    )
    try {
        // start
        val process = pb.start()
        val reader = BufferedReader(InputStreamReader(process.getInputStream()))
        var line: String?
        var foundGenerators = false
        while ((reader.readLine().also { line = it }) != null) {
            if (line == "Generators") {
                foundGenerators = true
            }
            if (foundGenerators) {
                project.getLogger().log(LogLevel.QUIET, line)
            }
        }
        process.waitFor()
    } catch (e: IOException) {
        throw GradleScriptException("cmake --help failed.", e)
    } catch (e: InterruptedException) {
        throw GradleScriptException("cmake --help failed.", e)
    }
}
cmakeGenerators.group = "cmake"
cmakeGenerators.description = "List available CMake generators"
project.afterEvaluate {
    val tasks = project.tasks
    if (extension.targets.targetContainer.isEmpty()) {
        tasks.register(cmakeConfigure, CMakeConfigureTask::class) {
            executable.set(extension.executable)
            workingFolder.set(extension.workingFolder)
            sourceFolder.set(extension.sourceFolder)
            configurationTypes.set(extension.configurationTypes)
            installPrefix.set(extension.installPrefix)
            generator.set(extension.generator)
            platform.set(extension.platform)
            toolset.set(extension.toolset)
            buildSharedLibs.set(extension.buildSharedLibs)
            buildStaticLibs.set(extension.buildStaticLibs)
            def.set(if (extension.defs.isPresent()) extension.defs else extension.def)
        }

        tasks.register(cmakeBuild, CMakeBuildTask::class) {
            executable.set(extension.executable)
            workingFolder.set(extension.workingFolder)
            buildConfig.set(extension.buildConfig)
            buildTarget.set(extension.buildTarget)
            buildClean.set(extension.buildClean)
        }
    } else {
        extension.targets.targetContainer.getAsMap().forEach { name, target ->
            tasks.register(cmakeConfigure + name, CMakeConfigureTask::class.java) {
                configureFromProject()
                if (target.executable.isPresent) executable.set(target.executable)
                if (target.workingFolder.isPresent) workingFolder.set(target.workingFolder)
                if (target.sourceFolder.isPresent) sourceFolder.set(target.sourceFolder)
                if (target.configurationTypes.isPresent) configurationTypes.set(target.configurationTypes)
                if (target.installPrefix.isPresent) installPrefix.set(target.installPrefix)
                if (target.generator.isPresent) generator.set(target.generator)
                if (target.platform.isPresent) platform.set(target.platform)
                if (target.toolset.isPresent) toolset.set(target.toolset)
                if (target.buildSharedLibs.isPresent) buildSharedLibs.set(target.buildSharedLibs)
                if (target.buildStaticLibs.isPresent) buildStaticLibs.set(target.buildStaticLibs)
                if (target.defs.isPresent) def.set(target.defs)
            }

            tasks.register(cmakeBuild + name, CMakeBuildTask::class.java) {
                configureFromProject()
                if (target.executable.isPresent) executable.set(target.executable)
                if (target.workingFolder.isPresent) workingFolder.set(target.workingFolder)
                if (target.buildConfig.isPresent) buildConfig.set(target.buildConfig)
                if (target.buildTarget.isPresent) buildTarget.set(target.buildTarget)
                if (target.buildClean.isPresent) buildClean.set(target.buildClean)
            }
        }
    }
    tasks.withType(CMakeBuildTask::class)
        .configureEach {
            dependsOn(
                tasks.withType(
                    CMakeConfigureTask::class.java
                )
            )
        }
    tasks.named("clean").configure { dependsOn("cmakeClean") }
    tasks.named("build").configure {
        dependsOn(
            tasks.withType(CMakeBuildTask::class)
        )
    }
}