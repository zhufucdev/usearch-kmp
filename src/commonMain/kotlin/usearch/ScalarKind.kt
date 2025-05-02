package usearch

expect enum class ScalarKind {
    /**
     * 64-bit double-precision IEEE 754 floating-point number.
     */
    F64,

    /**
     * 32-bit single-precision IEEE 754 floating-point number.
     */
    F32,

    /**
     * 16-bit half-precision IEEE 754 floating-point number.
     */
    F16,

    /**
     * 8-bit signed integer.
     */
    I8,

    /**
     * 1-bit binary value, packed 8 per byte.
     */
    B1
}