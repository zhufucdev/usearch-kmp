import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.Incremental
import java.io.File

abstract class DefFileTask : DefaultTask() {
    @get:InputFiles
    @get:Incremental
    abstract val staticLinkedBinary: Property<FileCollection>

    @get:InputFile
    @get:Incremental
    abstract val headerFile: Property<File>

    @get:OutputFile
    abstract val outputFile: Property<File>

    init {
        headerFile.set(project.file("src/cppMain/lib.h"))
        outputFile.set(project.layout.buildDirectory.file("lib.def").get().asFile)
    }

    @TaskAction
    fun writeDefFile() {
        outputFile.get().writeText(buildString {
            appendLine("headers = ${headerFile.get()}")
            appendLine("staticLibraries = ${staticLinkedBinary.get().joinToString { it.name }}")
            appendLine("libraryPaths = ${staticLinkedBinary.get().map { it.parent }.toSet().joinToString()}")
        })
    }
}