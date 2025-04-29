package usearch

object NativeMethods {
    val bridge = NativeBridge()
    init {
        platformLoadLib()
    }
}
