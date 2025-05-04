import groovy.lang.Closure
import groovy.lang.MissingMethodException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import javax.inject.Inject

abstract class TargetListExtension @Inject constructor(project: Project) {
    val targetContainer: NamedDomainObjectContainer<TargetExtension> = project.container(
        TargetExtension::class.java
    ) { name ->
        TargetExtension(project, name)
    }

    fun methodMissing(name: String, args: Any): Any {
        if (args is Array<*> && args.isArrayOf<Any>() && args[0] is Closure<*>) {
            return targetContainer.create(name, args[0] as Closure<*>)
        } else {
            val normalizedArgs: Array<*> = if (args is Array<*> && args.isArrayOf<Any>()) args else arrayOf(args)
            throw MissingMethodException(name, this.javaClass, normalizedArgs)
        }
    }
}
