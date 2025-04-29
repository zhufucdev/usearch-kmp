import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.work.Incremental

abstract class JniHeaderTask : Exec() {
    @get:InputFiles
    @get:Incremental
    abstract val sourceFiles: Property<FileCollection>

    @get:OutputDirectory
    abstract val headerOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val classOutputDir: DirectoryProperty

    init {
        classOutputDir.value(project.layout.buildDirectory.dir("classes/bridge"))
    }

    override fun exec() {
        workingDir = classOutputDir.asFile.get()
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
        executable = "javac"
        setArgs(buildList {
            add("-h")
            add(headerOutputDir.get().asFile.path)
            add("-d")
            add(classOutputDir.asFile.get().path)
            addAll(sourceFiles.get().map { it.path })
        })

        super.exec()
    }
}