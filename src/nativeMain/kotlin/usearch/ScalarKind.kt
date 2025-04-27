package usearch

import kotlinx.cinterop.ExperimentalForeignApi
import lib.*

@OptIn(ExperimentalForeignApi::class)
actual enum class ScalarKind(val nativeEnum: UInt) {
    F64(usearch_scalar_f64_k), F32(usearch_scalar_f32_k), F16(usearch_scalar_f16_k),
    I8(usearch_scalar_i8_k), B1(usearch_scalar_b1_k)
}