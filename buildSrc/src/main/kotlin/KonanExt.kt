import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.konan.target.KonanTarget

internal val KonanTarget.bigCamelName get() = name.split('_').joinToString("") { it.uppercaseFirstChar() }
internal val KonanTarget.smallCamelName get() = name.split('_').let {
    it.first() + it.subList(1, it.size).joinToString("") { e -> e.uppercaseFirstChar() }
}
