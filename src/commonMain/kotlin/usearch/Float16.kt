package usearch

import kotlin.jvm.JvmInline

/**
 * A data class representing a half-precision floating-point number based on the IEEE 754 standard.
 *
 * This class uses a Short to store the half-precision floating-point data, which includes 1 sign bit,
 * 5 exponent bits, and 10 mantissa bits.
 */
@JvmInline
value class Float16(internal val rawBits: Short) {
    companion object {
        internal const val SIGN_SHIFT = 15
        internal const val EXPONENT_SHIFT = 10
        internal const val MANTISSA_BITS = 10
        internal const val EXPONENT_BITS = 5
        internal const val MANTISSA_MASK = (1 shl EXPONENT_SHIFT) - 1          // 0x03FF (10 bits)
        internal const val EXPONENT_MASK = (1 shl EXPONENT_BITS) - 1           // 0x001F (5 bits)
        internal const val SIGN_MASK_INT = 1 shl SIGN_SHIFT                    // 0x8000
        internal const val SIGN_MASK_SHORT = SIGN_MASK_INT.toShort()

        internal const val BIAS = 15
        internal const val MAX_EXPONENT_VALUE = (1 shl EXPONENT_BITS) - 1 // 31 (Used for Inf/NaN)
        internal const val MIN_NORMAL_EXPONENT_VALUE = 1
        internal const val MAX_NORMAL_EXPONENT_VALUE = MAX_EXPONENT_VALUE - 1 // 30

        // Special FP16 bit patterns
        internal const val POSITIVE_INFINITY_BITS: Short = ((MAX_EXPONENT_VALUE shl EXPONENT_SHIFT)).toShort() // 0x7C00
        internal const val NEGATIVE_INFINITY_BITS: Short =
            (SIGN_MASK_INT or (MAX_EXPONENT_VALUE shl EXPONENT_SHIFT)).toShort() // 0xFC00

        // A common quiet NaN pattern (exponent all 1s, MSB of mantissa is 1)
        internal const val NaN_BITS: Short =
            ((MAX_EXPONENT_VALUE shl EXPONENT_SHIFT) or (1 shl (MANTISSA_BITS - 1))).toShort() // 0x7E00
        // Another possible NaN (more specific than just non-zero mantissa)
        // const val NaN_BITS: Short = ((MAX_EXPONENT_VALUE shl EXPONENT_SHIFT) or 1).toShort() // 0x7C01

        // Publicly accessible Float16 instances for special values
        val POSITIVE_INFINITY = Float16(POSITIVE_INFINITY_BITS)
        val NEGATIVE_INFINITY = Float16(NEGATIVE_INFINITY_BITS)
        val NaN = Float16(NaN_BITS)
        val POSITIVE_ZERO = Float16(0)
        val NEGATIVE_ZERO = Float16(SIGN_MASK_SHORT)

        // Max normal value: sign=0, exp=30 (biased 11110), mantissa=all 1s
        val MAX_VALUE =
            Float16(((MAX_NORMAL_EXPONENT_VALUE shl EXPONENT_SHIFT) or MANTISSA_MASK).toShort()) // 65504, 0x7BFF

        // Min normal value: sign=0, exp=1 (biased 00001), mantissa=all 0s
        val MIN_NORMAL_VALUE =
            Float16((MIN_NORMAL_EXPONENT_VALUE shl EXPONENT_SHIFT).toShort()) // 2^-14 ≈ 6.1035e-5, 0x0400

        // Smallest positive subnormal value: sign=0, exp=0, mantissa=1
        val MIN_VALUE = Float16(1) // 2^-24 ≈ 5.9604e-8, 0x0001

        // Largest subnormal value: sign=0, exp=0, mantissa=all 1s
        val MAX_SUBNORMAL_VALUE = Float16(MANTISSA_MASK.toShort()) // ≈ 6.1004e-5, 0x03FF

        // --- FP32 (Single-Precision / Float) Constants ---
        internal const val FLOAT_SIGN_SHIFT = 31
        internal const val FLOAT_EXPONENT_SHIFT = 23
        internal const val FLOAT_MANTISSA_BITS = 23
        internal const val FLOAT_EXPONENT_BITS = 8
        internal const val FLOAT_MANTISSA_MASK = (1 shl FLOAT_EXPONENT_SHIFT) - 1          // 0x007FFFFF (23 bits)
        internal const val FLOAT_EXPONENT_MASK = (1 shl FLOAT_EXPONENT_BITS) - 1           // 0x000000FF (8 bits)
        internal const val FLOAT_SIGN_MASK_INT = 1 shl FLOAT_SIGN_SHIFT                    // 0x80000000

        internal const val FLOAT_BIAS = 127
        internal const val FLOAT_MAX_EXPONENT_VALUE = (1 shl FLOAT_EXPONENT_BITS) - 1 // 255

        // --- FP64 (Double-Precision / Double) Constants ---
        internal const val DOUBLE_SIGN_SHIFT = 63
        internal const val DOUBLE_EXPONENT_SHIFT = 52
        internal const val DOUBLE_MANTISSA_BITS = 52
        internal const val DOUBLE_EXPONENT_BITS = 11
        internal const val DOUBLE_MANTISSA_MASK = (1L shl DOUBLE_EXPONENT_SHIFT) - 1L        // 52 bits
        internal const val DOUBLE_EXPONENT_MASK = (1L shl DOUBLE_EXPONENT_BITS) - 1L         // 11 bits
        internal const val DOUBLE_SIGN_MASK_LONG = 1L shl DOUBLE_SIGN_SHIFT                  // 64th bit

        internal const val DOUBLE_BIAS = 1023
        internal const val DOUBLE_MAX_EXPONENT_VALUE = (1L shl DOUBLE_EXPONENT_BITS) - 1L // 2047
    }

    /**
     * Provides a string representation by converting to Float first.
     */
    override fun toString(): String {
        // Delegate to Float's toString for standard representation
        return this.toFloat().toString()
    }
}

/**
 * Converts a Float16 (half-precision) value to a Float (single-precision).
 */
fun Float16.toFloat(): Float {
    val bits16 = rawBits.toInt() and 0xFFFF // Work with 16 bits as unsigned int

    // Extract components from FP16
    val sign16 = bits16 ushr Float16.SIGN_SHIFT
    val exponent16 = (bits16 ushr Float16.EXPONENT_SHIFT) and Float16.EXPONENT_MASK
    val mantissa16 = bits16 and Float16.MANTISSA_MASK

    // Determine Float components
    val sign32 = sign16 shl Float16.FLOAT_SIGN_SHIFT // Shift sign bit to FP32 position
    var exponent32: Int
    var mantissa32: Int

    when (exponent16) {
        Float16.MAX_EXPONENT_VALUE -> { // Infinity or NaN
            exponent32 = Float16.FLOAT_MAX_EXPONENT_VALUE // Float exponent all 1s
            mantissa32 = if (mantissa16 == 0) {
                0 // Infinity
            } else {
                // Propagate NaN payload (mantissa bits) from FP16 to FP32
                // Shift left to align and potentially set the quiet NaN bit if needed
                mantissa16 shl (Float16.FLOAT_MANTISSA_BITS - Float16.MANTISSA_BITS)
            }
        }

        0 -> { // Zero or Subnormal FP16
            if (mantissa16 == 0) {
                // Zero
                exponent32 = 0
                mantissa32 = 0
            } else {
                // Subnormal FP16 -> Normalize to Float if possible
                // Value = (-1)^sign * 2^(1-bias16) * (0.mantissa16)_2
                //       = (-1)^sign * 2^(-14) * (mantissa16 / 2^10)

                // Find the leading '1' bit in the subnormal mantissa
                var leadingOnePos = -1
                for (i in Float16.MANTISSA_BITS - 1 downTo 0) {
                    if ((mantissa16 and (1 shl i)) != 0) {
                        leadingOnePos = i
                        break
                    }
                }

                // Calculate the normalization shift and the new exponent
                val shift =
                    Float16.MANTISSA_BITS - 1 - leadingOnePos // How many positions the leading 1 is from the MSB
                val effectiveExponent =
                    Float16.MIN_NORMAL_EXPONENT_VALUE - Float16.BIAS - shift // Exponent = 1 - 15 - shift = -14 - shift

                // Alternative subnormal calculation:
                // Keep shifting mantissa left until the implicit 1 is found (or mantissa becomes 0)
                var currentMantissa = mantissa16
                var currentExponent = 1 // Start with exponent for subnormals (1 - bias)
                while ((currentMantissa and (1 shl Float16.MANTISSA_BITS)) == 0) {
                    currentMantissa = currentMantissa shl 1
                    currentExponent--
                    if (currentExponent <= -Float16.FLOAT_BIAS) break // Avoid infinite loop / underflow to zero
                }

                if (currentExponent <= -Float16.FLOAT_BIAS || currentMantissa == 0) {
                    // Underflowed to zero
                    exponent32 = 0
                    mantissa32 = 0
                } else {
                    exponent32 = currentExponent + Float16.FLOAT_BIAS // Re-bias for Float
                    // Remove implicit 1, scale mantissa
                    mantissa32 =
                        (currentMantissa and Float16.MANTISSA_MASK) shl (Float16.FLOAT_MANTISSA_BITS - Float16.MANTISSA_BITS)
                }
            }
        }

        else -> { // Normalized FP16
            // Value = (-1)^sign * 2^(exponent16 - bias16) * (1.mantissa16)_2
            val unbiasedExponent = exponent16 - Float16.BIAS
            exponent32 = unbiasedExponent + Float16.FLOAT_BIAS // Re-bias for Float
            // Extend mantissa from 10 bits to 23 bits by padding with zeros
            mantissa32 = mantissa16 shl (Float16.FLOAT_MANTISSA_BITS - Float16.MANTISSA_BITS)
        }
    }

    // Assemble Float bits
    val bits32 = sign32 or (exponent32 shl Float16.FLOAT_EXPONENT_SHIFT) or mantissa32
    return Float.fromBits(bits32)
}


/**
 * Converts a Float16 (half-precision) value to a Double (double-precision).
 */
fun Float16.toDouble(): Double {
    val bits16 = rawBits.toInt() and 0xFFFF // Work with 16 bits as unsigned int

    // Extract components from FP16
    val sign16 = bits16 ushr Float16.SIGN_SHIFT
    val exponent16 = (bits16 ushr Float16.EXPONENT_SHIFT) and Float16.EXPONENT_MASK
    val mantissa16 = bits16 and Float16.MANTISSA_MASK

    // Determine Double components
    val sign64 = sign16.toLong() shl Float16.DOUBLE_SIGN_SHIFT // Shift sign bit to FP64 position
    var exponent64: Long
    var mantissa64: Long

    when (exponent16) {
        Float16.MAX_EXPONENT_VALUE -> { // Infinity or NaN
            exponent64 = Float16.DOUBLE_MAX_EXPONENT_VALUE // Double exponent all 1s
            mantissa64 = if (mantissa16 == 0) {
                0L // Infinity
            } else {
                // Propagate NaN payload, shifting left to align in 52 bits
                mantissa16.toLong() shl (Float16.DOUBLE_MANTISSA_BITS - Float16.MANTISSA_BITS)
            }
        }

        0 -> { // Zero or Subnormal FP16
            if (mantissa16 == 0) {
                // Zero
                exponent64 = 0L
                mantissa64 = 0L
            } else {
                // Subnormal FP16 -> Normalize to Double
                // Value = (-1)^sign * 2^(-14) * (mantissa16 / 2^10)
                // Find the leading '1' bit
                var currentMantissa = mantissa16
                var currentExponent = 1 // Start with exponent for subnormals (1 - bias)
                while ((currentMantissa and (1 shl Float16.MANTISSA_BITS)) == 0) {
                    currentMantissa = currentMantissa shl 1
                    currentExponent--
                    // No need to check for underflow here, Double has much wider range
                }

                exponent64 = (currentExponent + Float16.DOUBLE_BIAS).toLong() // Re-bias for Double
                // Remove implicit 1, scale mantissa to 52 bits
                mantissa64 =
                    (currentMantissa and Float16.MANTISSA_MASK).toLong() shl (Float16.DOUBLE_MANTISSA_BITS - Float16.MANTISSA_BITS)

            }
        }

        else -> { // Normalized FP16
            // Value = (-1)^sign * 2^(exponent16 - bias16) * (1.mantissa16)_2
            val unbiasedExponent = exponent16 - Float16.BIAS
            exponent64 = (unbiasedExponent + Float16.DOUBLE_BIAS).toLong() // Re-bias for Double
            // Extend mantissa from 10 bits to 52 bits by padding with zeros
            mantissa64 = mantissa16.toLong() shl (Float16.DOUBLE_MANTISSA_BITS - Float16.MANTISSA_BITS)
        }
    }

    // Assemble Double bits
    val bits64 = sign64 or (exponent64 shl Float16.DOUBLE_EXPONENT_SHIFT) or mantissa64
    return Double.fromBits(bits64)
}

/**
 * Converts a Float (single-precision) value to a Float16 (half-precision).
 * Handles special values, overflow, underflow (rounds to zero or subnormal), and rounding (round-to-nearest-even).
 * This is a corrected implementation compared to the one in the prompt.
 */
fun Float.toFloat16(): Float16 {
    val bits32 = this.toRawBits()

    // Extract components from Float
    val sign32 = bits32 ushr Float16.FLOAT_SIGN_SHIFT
    val exponent32 = (bits32 ushr Float16.FLOAT_EXPONENT_SHIFT) and Float16.FLOAT_EXPONENT_MASK
    val mantissa32 = bits32 and Float16.FLOAT_MANTISSA_MASK

    // Determine Float16 components
    val sign16 = sign32 // Sign bit position is different, but value is the same (0 or 1)
    var exponent16: Int
    var mantissa16: Int

    when (exponent32) {
        Float16.FLOAT_MAX_EXPONENT_VALUE -> { // Float Infinity or NaN
            exponent16 = Float16.MAX_EXPONENT_VALUE
            mantissa16 = if (mantissa32 == 0) {
                0 // Infinity
            } else {
                // Propagate NaN payload, taking the top 10 bits of the Float mantissa
                // Set MSB of FP16 mantissa to ensure it's NaN if Float mantissa was non-zero
                (mantissa32 ushr (Float16.FLOAT_MANTISSA_BITS - Float16.MANTISSA_BITS)) or (1 shl (Float16.MANTISSA_BITS - 1)) // Ensure NaN
            }
        }

        0 -> { // Float Zero or Subnormal
            // Float subnormals are smaller than the smallest FP16 subnormal, they become FP16 zero
            exponent16 = 0
            mantissa16 = 0
        }

        else -> { // Normalized Float
            val unbiasedExponent = exponent32 - Float16.FLOAT_BIAS

            when {
                unbiasedExponent > Float16.BIAS -> { // Overflow to FP16 Infinity
                    exponent16 = Float16.MAX_EXPONENT_VALUE
                    mantissa16 = 0
                }

                unbiasedExponent < (Float16.MIN_NORMAL_EXPONENT_VALUE - Float16.BIAS) -> { // Underflow to FP16 Subnormal or Zero
                    // Value is too small to be a normal FP16 number. Check if it can be subnormal.
                    // Smallest normal FP16 exponent = 1 - 15 = -14
                    // Calculate the required shift to represent as subnormal
                    val shift =
                        Float16.FLOAT_MANTISSA_BITS - (unbiasedExponent - (Float16.MIN_NORMAL_EXPONENT_VALUE - Float16.BIAS - Float16.MANTISSA_BITS))
                    // shift = 23 - (exp - (-14 - 10)) = 23 - (exp + 24) = -1 - exp

                    // Add implicit 1 back to mantissa for subnormal calculation
                    val implicitMantissa32 = mantissa32 or (1 shl Float16.FLOAT_MANTISSA_BITS)

                    // Check if the number is too small even for subnormals (conservative check: exponent < -24)
                    if (unbiasedExponent < (Float16.MIN_NORMAL_EXPONENT_VALUE - Float16.BIAS - Float16.MANTISSA_BITS)) { // exp < 1-15-10 = -24
                        exponent16 = 0
                        mantissa16 = 0
                    } else {
                        // Attempt to represent as subnormal, requires right shifting
                        val subnormalShift =
                            (Float16.MIN_NORMAL_EXPONENT_VALUE - Float16.BIAS) - unbiasedExponent // 1-15 - exp = -14 - exp
                        if (subnormalShift > Float16.FLOAT_MANTISSA_BITS) { // Shifting out all bits?
                            exponent16 = 0
                            mantissa16 = 0
                        } else {
                            // Perform rounding during the shift
                            val shiftedMantissa = implicitMantissa32 ushr subnormalShift
                            val roundBitMask = 1 shl (subnormalShift - 1)
                            val stickyMask = roundBitMask - 1

                            val roundBit = (implicitMantissa32 and roundBitMask) != 0
                            val stickyBits = (implicitMantissa32 and stickyMask) != 0

                            exponent16 = 0 // Subnormal exponent is always 0
                            mantissa16 = shiftedMantissa

                            // Round-to-nearest-even
                            if (roundBit && (stickyBits || (mantissa16 and 1) != 0)) {
                                mantissa16 += 1
                                // If rounding causes mantissa to overflow (>= 1024), it becomes the smallest normal number
                                if (mantissa16 >= (1 shl Float16.MANTISSA_BITS)) {
                                    exponent16 = 1 // Smallest normal exponent
                                    mantissa16 = 0 // Mantissa becomes 0
                                }
                            }
                        }
                    }
                }

                else -> { // Normal FP16 range
                    exponent16 = unbiasedExponent + Float16.BIAS // Re-bias for FP16
                    // Need to reduce mantissa from 23 bits to 10 bits, applying rounding
                    val shift = Float16.FLOAT_MANTISSA_BITS - Float16.MANTISSA_BITS // 23 - 10 = 13

                    // Round-to-nearest-even
                    val roundBitMask = 1 shl (shift - 1) // Bit 12 (0-indexed) is the rounding bit
                    val stickyMask = roundBitMask - 1    // Bits 0 to 11 are sticky bits

                    val roundBit = (mantissa32 and roundBitMask) != 0
                    val stickyBits = (mantissa32 and stickyMask) != 0

                    mantissa16 = mantissa32 ushr shift // Truncated mantissa

                    if (roundBit && (stickyBits || (mantissa16 and 1) != 0)) {
                        mantissa16 += 1
                        // Check for mantissa overflow (carry to exponent)
                        if (mantissa16 == (1 shl Float16.MANTISSA_BITS)) { // e.g., 0x3FF + 1 = 0x400
                            mantissa16 = 0
                            exponent16 += 1
                            // Check for exponent overflow (carry to infinity)
                            if (exponent16 == Float16.MAX_EXPONENT_VALUE) {
                                mantissa16 = 0 // Ensure mantissa is 0 for infinity
                            }
                        }
                    }
                }
            }
        }
    }

    // Assemble Float16 bits
    val bits16 = (sign16 shl Float16.SIGN_SHIFT) or (exponent16 shl Float16.EXPONENT_SHIFT) or mantissa16
    return Float16(bits16.toShort())
}


/**
 * Converts a Double (double-precision) value to a Float16 (half-precision).
 * Handles special values, overflow, underflow (rounds to zero or subnormal), and rounding (round-to-nearest-even).
 */
fun Double.toFloat16(): Float16 {
    val bits64 = this.toRawBits()

    // Extract components from Double
    val sign64 = (bits64 ushr Float16.DOUBLE_SIGN_SHIFT).toInt() // Get sign as 0 or 1
    val exponent64 = ((bits64 ushr Float16.DOUBLE_EXPONENT_SHIFT) and Float16.DOUBLE_EXPONENT_MASK).toInt()
    val mantissa64 = bits64 and Float16.DOUBLE_MANTISSA_MASK

    // Determine Float16 components
    val sign16 = sign64
    var exponent16: Int
    var mantissa16: Int

    when (exponent64) {
        Float16.DOUBLE_MAX_EXPONENT_VALUE.toInt() -> { // Double Infinity or NaN
            exponent16 = Float16.MAX_EXPONENT_VALUE
            mantissa16 = if (mantissa64 == 0L) {
                0 // Infinity
            } else {
                // Propagate NaN, taking top 10 bits of Double mantissa
                (mantissa64 ushr (Float16.DOUBLE_MANTISSA_BITS - Float16.MANTISSA_BITS)).toInt() or (1 shl (Float16.MANTISSA_BITS - 1)) // Ensure NaN
            }
        }

        0 -> { // Double Zero or Subnormal
            // Double subnormals are much smaller than FP16 subnormals, map to FP16 zero
            exponent16 = 0
            mantissa16 = 0
        }

        else -> { // Normalized Double
            val unbiasedExponent = exponent64 - Float16.DOUBLE_BIAS

            when {
                unbiasedExponent > Float16.BIAS -> { // Overflow to FP16 Infinity
                    exponent16 = Float16.MAX_EXPONENT_VALUE
                    mantissa16 = 0
                }

                unbiasedExponent < (Float16.MIN_NORMAL_EXPONENT_VALUE - Float16.BIAS - Float16.MANTISSA_BITS) -> { // Underflow to FP16 Zero
                    // Check if exponent < -24 (conservative check for underflow)
                    exponent16 = 0
                    mantissa16 = 0
                }

                unbiasedExponent <= (Float16.MIN_NORMAL_EXPONENT_VALUE - Float16.BIAS) -> { // Underflow to FP16 Subnormal or Zero
                    // Value might be representable as FP16 subnormal
                    // Add implicit 1 back to mantissa
                    val implicitMantissa64 = mantissa64 or (1L shl Float16.DOUBLE_MANTISSA_BITS)
                    // Calculate shift needed to align for subnormal representation
                    val subnormalShift =
                        (Float16.MIN_NORMAL_EXPONENT_VALUE - Float16.BIAS) - unbiasedExponent // -14 - exp

                    if (subnormalShift > Float16.DOUBLE_MANTISSA_BITS + 1) { // Shifting out all bits? (+1 for implicit bit)
                        exponent16 = 0
                        mantissa16 = 0
                    } else {
                        // Perform rounding during the shift right
                        val effectiveShift =
                            subnormalShift + (Float16.DOUBLE_MANTISSA_BITS - Float16.MANTISSA_BITS) // Total shift from original mantissa
                        // effectiveShift = -14 - exp + 42 = 28 - exp

                        val roundBitIndex = effectiveShift - 1
                        val stickyMask = (1L shl roundBitIndex) - 1L

                        val roundBit = (implicitMantissa64 ushr roundBitIndex) and 1L != 0L
                        val stickyBits = (implicitMantissa64 and stickyMask) != 0L

                        exponent16 = 0 // Subnormal exponent
                        mantissa16 = (implicitMantissa64 ushr effectiveShift).toInt()

                        // Round-to-nearest-even
                        if (roundBit && (stickyBits || (mantissa16 and 1) != 0)) {
                            mantissa16 += 1
                            // If rounding causes mantissa to overflow, it becomes the smallest normal number
                            if (mantissa16 >= (1 shl Float16.MANTISSA_BITS)) {
                                exponent16 = 1 // Smallest normal exponent
                                mantissa16 = 0 // Mantissa becomes 0
                            }
                        }
                    }
                }

                else -> { // Normal FP16 range
                    exponent16 = unbiasedExponent + Float16.BIAS // Re-bias for FP16
                    // Reduce mantissa from 52 bits to 10 bits, applying rounding
                    val shift = Float16.DOUBLE_MANTISSA_BITS - Float16.MANTISSA_BITS // 52 - 10 = 42

                    // Round-to-nearest-even
                    val roundBitIndex = shift - 1 // Bit 41 (0-indexed) is the rounding bit
                    val roundBitMask = 1L shl roundBitIndex
                    val stickyMask = roundBitMask - 1L    // Bits 0 to 40 are sticky bits

                    val roundBit = (mantissa64 and roundBitMask) != 0L
                    val stickyBits = (mantissa64 and stickyMask) != 0L

                    mantissa16 = (mantissa64 ushr shift).toInt() // Truncated mantissa

                    if (roundBit && (stickyBits || (mantissa16 and 1) != 0)) {
                        mantissa16 += 1
                        // Check for mantissa overflow (carry to exponent)
                        if (mantissa16 == (1 shl Float16.MANTISSA_BITS)) {
                            mantissa16 = 0
                            exponent16 += 1
                            // Check for exponent overflow (carry to infinity)
                            if (exponent16 == Float16.MAX_EXPONENT_VALUE) {
                                mantissa16 = 0 // Ensure mantissa is 0 for infinity
                            }
                        }
                    }
                }
            }
        }
    }

    // Assemble Float16 bits
    val bits16 = (sign16 shl Float16.SIGN_SHIFT) or (exponent16 shl Float16.EXPONENT_SHIFT) or mantissa16
    return Float16(bits16.toShort())
}

fun Float16.isNaN() = rawBits == Float16.NaN_BITS

fun Float16.isInfinite() = rawBits == Float16.NEGATIVE_INFINITY_BITS || rawBits == Float16.POSITIVE_INFINITY_BITS

fun Float16.isFinite() = !isInfinite()

/**
 * Get the internal binary representation.
 */
fun Float16.toRawBits() = rawBits

class Float16Array : Iterable<Float16> {
    internal val inner: ShortArray

    constructor(rawBits: ShortArray) {
        this.inner = rawBits
    }

    constructor(count: Int) : this(ShortArray(count))

    constructor(count: Int, init: (Int) -> Float16) : this(ShortArray(count) { init(it).rawBits })

    operator fun get(index: Int) = Float16(inner[index])

    operator fun set(index: Int, value: Float16) {
        inner[index] = value.rawBits
    }

    val size get() = inner.size

    private inner class Float16IteratorImpl : Float16Iterator() {
        private var current = 0

        override fun nextFloat16(): Float16 = get(current++)

        override fun hasNext(): Boolean = current >= size
    }

    override operator fun iterator(): Float16Iterator = Float16IteratorImpl()
}

/**
 * Returns `true` if the array is empty.
 */
fun Float16Array.isEmpty() = size <= 0

/**
 * Returns `true` if the array is not empty.
 */
fun Float16Array.isNotEmpty() = size > 0


/**
 * Get the underlying presentation, whose mutation will be reflected
 * in the original float16 array. If this is not the desired behavior,
 * use [toShortArray] instead.
 */
fun Float16Array.toRawBits() = inner

/**
 * Get the underlying presentation.
 */
fun Float16Array.toShortArray() = ShortArray(inner.size) { inner[it] }

fun Float16Array.sliceArray(indices: IntRange) = Float16Array(inner.sliceArray(indices))
fun Float16Array.sliceArray(indices: Collection<Int>) = Float16Array(inner.sliceArray(indices))

abstract class Float16Iterator : Iterator<Float16> {
    final override fun next(): Float16 = nextFloat16()

    abstract fun nextFloat16(): Float16
}