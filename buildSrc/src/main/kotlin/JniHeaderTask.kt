import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.compile.JavaCompile

abstract class JniHeaderTask : JavaCompile() {
    @get:OutputDirectory
    abstract val headerOutputDir: DirectoryProperty

    init {
        destinationDirectory.value(project.layout.buildDirectory.dir("classes/bridge"))
        options.headerOutputDirectory.value(headerOutputDir)
    }
}