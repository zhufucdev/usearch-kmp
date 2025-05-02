import usearch.Float16
import usearch.toFloat16
import usearch.toRawBits
import kotlin.test.Test
import kotlin.test.assertEquals

class Float16Test {
    @Test
    fun fromF32() {
        assertEquals(14336, 0.5f.toFloat16().toRawBits())
        assertEquals(15872, 1.5f.toFloat16().toRawBits())
        assertEquals(Float16.NaN, Float.NaN.toFloat16())
        assertEquals(Float16.POSITIVE_INFINITY, Float.POSITIVE_INFINITY.toFloat16())
        assertEquals(Float16.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY.toFloat16())
    }

    @Test
    fun fromF64() {
        assertEquals(14336, 0.5.toFloat16().toRawBits())
        assertEquals(15872, 1.5.toFloat16().toRawBits())
        assertEquals(Float16.POSITIVE_INFINITY, Double.POSITIVE_INFINITY.toFloat16())
        assertEquals(Float16.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY.toFloat16())
    }
}