package usearch

actual enum class ScalarKind(val nativeEnum: Int) {
    F64(2), F32(1), F16(3), I8(4), B1(5)
}