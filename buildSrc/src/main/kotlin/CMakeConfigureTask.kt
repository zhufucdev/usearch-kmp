import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

abstract class CMakeConfigureTask : DefaultTask() {
    /** region getters */
    @get:Optional
    @get:Input
    abstract val executable: Property<String>

    @get:OutputDirectory
    abstract val workingFolder: DirectoryProperty

    @get:InputDirectory
    abstract val sourceFolder: DirectoryProperty

    @get:Optional
    @get:Input
    abstract val configurationTypes: Property<String>

    @get:Optional
    @get:Input
    abstract val installPrefix: Property<String>

    @get:Optional
    @get:Input
    abstract val generator: Property<String> // for example: "Visual Studio 16 2019"

    @get:Optional
    @get:Input
    abstract val platform: Property<String> // for example "x64" or "Win32" or "ARM" or "ARM64", supported on vs > 8.0

    @get:Optional
    @get:Input
    abstract val toolset: Property<String> // for example "v142", supported on vs > 10.0

    @get:Optional
    @get:Input
    abstract val buildSharedLibs: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val buildStaticLibs: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val def: MapProperty<String, String>

    init {
        group = "cmake"
        description = "Configure a Build with CMake"
        // default values
        workingFolder.set(File(project.layout.buildDirectory.get().asFile, "cmake"))
        sourceFolder.set(
            File(
                project.layout.buildDirectory.get().asFile,
                "src" + File.separator + "main" + File.separator + "cpp"
            )
        )
    }

    fun configureFromProject() {
        val ext = project.extensions.getByName("cmake") as CMakePluginExtension
        executable.value(ext.executable)
        workingFolder.value(ext.workingFolder)
        sourceFolder.value(ext.sourceFolder)
        configurationTypes.value(ext.configurationTypes)
        installPrefix.value(ext.installPrefix)
        generator.value(ext.generator)
        platform.value(ext.platform)
        toolset.value(ext.toolset)
        buildSharedLibs.value(ext.buildSharedLibs)
        buildStaticLibs.value(ext.buildStaticLibs)
        def.value(ext.defs)
    }

    /** endregion */
    open fun buildCmdLine(): MutableList<String> {
        val parameters: MutableList<String> = ArrayList()

        parameters.add(executable.getOrElse("cmake"))

        if (generator.isPresent && !generator.get().isEmpty()) {
            parameters.add("-G")
            parameters.add(generator.get())
        }

        if (platform.isPresent && !platform.get().isEmpty()) {
            parameters.add("-A")
            parameters.add(platform.get())
        }

        if (toolset.isPresent && !toolset.get().isEmpty()) {
            parameters.add("-T")
            parameters.add(toolset.get())
        }

        if (configurationTypes.isPresent && !configurationTypes.get()
                .isEmpty()
        ) parameters.add("-DCMAKE_CONFIGURATION_TYPES=" + configurationTypes.get())

        if (installPrefix.isPresent && !installPrefix.get()
                .isEmpty()
        ) parameters.add("-DCMAKE_INSTALL_PREFIX=" + installPrefix.get())


        if (buildSharedLibs.isPresent) parameters.add("-DBUILD_SHARED_LIBS=" + (if (buildSharedLibs.get()) "ON" else "OFF"))

        if (buildStaticLibs.isPresent) parameters.add("-DBUILD_STATIC_LIBS=" + (if (buildStaticLibs.get()) "ON" else "OFF"))


        if (def.isPresent) {
            for (entry in def.get().entries) parameters.add("-D" + entry.key + "=" + entry.value)
        }

        parameters.add(sourceFolder.asFile.get().absolutePath)

        return parameters
    }

    @TaskAction
    fun configure() {
        val executor = CMakeExecutor(logger, name)
        executor.exec(buildCmdLine(), workingFolder.asFile.get())
    }
}
