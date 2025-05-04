import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import java.io.File

abstract class CMakePluginExtension(project: Project) {
    // parameters used by config and build step
    abstract val executable: Property<String>

    abstract val workingFolder: DirectoryProperty

    // parameters used by config step
    abstract val sourceFolder: DirectoryProperty

    abstract val configurationTypes: Property<String>

    abstract val installPrefix: Property<String>

    abstract val generator: Property<String> // for example: "Visual Studio 16 2019"


    abstract val platform: Property<String> // for example "x64" or "Win32" or "ARM" or "ARM64", supported on vs > 8.0

    abstract val toolset: Property<String> // for example "v142", supported on vs > 10.0

    abstract val buildSharedLibs: Property<Boolean>

    abstract val buildStaticLibs: Property<Boolean>

    abstract val defs: MapProperty<String, String>

    abstract val def: MapProperty<String, String>

    // parameters used on build step
    abstract val buildConfig: Property<String>

    abstract val buildTarget: Property<String>

    abstract val buildClean: Property<Boolean>

    val targets: TargetListExtension = project.objects.newInstance(TargetListExtension::class.java, project)
    private val project: Project

    init {
        // default values
        workingFolder.set(File(project.layout.buildDirectory.get().asFile, "cmake"))
        sourceFolder.set(
            File(
                project.layout.buildDirectory.get().asFile,
                "src" + File.separator + "main" + File.separator + "cpp"
            )
        )
        this.project = project
    }
}
