import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Build a configured Build with CMake
 */
abstract class CMakeBuildTask : DefaultTask() {
    @get:Optional
    @get:Input
    abstract val konanTarget: Property<KonanTarget>

    @get:Optional
    @get:Input
    abstract val executable: Property<String>

    @get:InputDirectory
    abstract val workingFolder: DirectoryProperty

    @get:Optional
    @get:Input
    abstract val buildConfig: Property<String>

    @get:Optional
    @get:Input
    abstract val buildTarget: Property<String>

    @get:Optional
    @get:Input
    abstract val buildClean: Property<Boolean>

    init {
        group = "cmake"
        description = "Build a configured Build with CMake"
    }

    fun configureFromProject() {
        val ext = project.extensions.getByName("cmake") as CMakePluginExtension
        executable.set(ext.executable)
        workingFolder.set(ext.workingFolder)
        buildConfig.set(ext.buildConfig)
        buildTarget.set(ext.buildTarget)
        buildClean.set(ext.buildClean)
    }

    open fun buildCmdLine(): MutableList<String> {
        val parameters: MutableList<String> = ArrayList()
        parameters.add(executable.getOrElse("cmake"))
        parameters.add("--build")
        parameters.add(".")
        if (buildConfig.isPresent()) {
            parameters.add("--config")
            parameters.add(buildConfig.get())
        }
        if (buildTarget.isPresent()) {
            parameters.add("--target")
            parameters.add(buildTarget.get())
        }
        if (buildClean.getOrElse(false)) {
            parameters.add("--clean-first")
        }
        return parameters
    }

    @TaskAction
    fun build() {
        val executor = CMakeExecutor(getLogger(), getName())
        executor.exec(buildCmdLine(), workingFolder.getAsFile().get())
    }
}
