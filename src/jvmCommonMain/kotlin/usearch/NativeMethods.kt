package usearch

object NativeMethods {
    val bridge = NativeBridge() // TODO: IDEA is unable to register this Gradle directory dependency
    init {
        platformLoadLib()
    }
}
