import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import java.io.File

class TargetExtension(project: Project, val name: String) {
    // parameters used by config and build step
     val executable: Property<String> = project.objects.property()
     val workingFolder: DirectoryProperty = project.objects.directoryProperty()

    // parameters used by config step
     val sourceFolder: DirectoryProperty = project.objects.directoryProperty()
     val configurationTypes: Property<String> = project.objects.property()
     val installPrefix: Property<String> = project.objects.property()
     val generator: Property<String> = project.objects.property() // for example: "Visual Studio 16 2019"
     val platform: Property<String> = project.objects.property() // for example "x64" or "Win32" or "ARM" or "ARM64", supported on vs > 8.0
     val toolset: Property<String> = project.objects.property() // for example "v142", supported on vs > 10.0
     val buildSharedLibs: Property<Boolean> = project.objects.property()
     val buildStaticLibs: Property<Boolean> = project.objects.property()
     val defs: MapProperty<String, String> = project.objects.mapProperty()

    // parameters used on build step
     val buildConfig: Property<String> = project.objects.property()
     val buildTarget: Property<String> = project.objects.property()
     val buildClean: Property<Boolean> = project.objects.property()

    fun setExecutable(executable: String) {
        this.executable.set(executable)
    }

    fun setWorkingFolder(workingFolder: File) {
        this.workingFolder.set(workingFolder)
    }

    fun setWorkingFolder(workingFolder: String) {
        this.workingFolder.set(File(workingFolder))
    }

    fun setSourceFolder(sourceFolder: File) {
        this.sourceFolder.set(sourceFolder)
    }

    fun setSourceFolder(sourceFolder: String) {
        this.sourceFolder.set(File(sourceFolder))
    }

    fun setConfigurationTypes(configurationTypes: String) {
        this.configurationTypes.set(configurationTypes)
    }

    fun setInstallPrefix(installPrefix: String) {
        this.installPrefix.set(installPrefix)
    }

    fun setGenerator(generator: String) {
        this.generator.set(generator)
    }

    fun setPlatform(platform: String) {
        this.platform.set(platform)
    }

    fun setToolset(toolset: String) {
        this.toolset.set(toolset)
    }

    fun setBuildSharedLibs(buildSharedLibs: Boolean) {
        this.buildSharedLibs.set(buildSharedLibs)
    }

    fun setBuildStaticLibs(buildStaticLibs: Boolean) {
        this.buildStaticLibs.set(buildStaticLibs)
    }

    fun setDefs(defs: MutableMap<String, String>) {
        this.defs.set(defs)
    }

    fun setBuildConfig(buildConfig: String) {
        this.buildConfig.set(buildConfig)
    }

    fun setBuildTarget(buildTarget: String) {
        this.buildTarget.set(buildTarget)
    }

    fun setBuildClean(buildClean: Boolean) {
        this.buildClean.set(buildClean)
    }
}
