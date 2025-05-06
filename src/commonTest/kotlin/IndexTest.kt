import usearch.Float16Array
import usearch.Index
import usearch.IndexOptions
import usearch.MetricKind
import usearch.ScalarKind
import usearch.toFloat16
import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class IndexTest {
    private val exampleOpts get() = IndexOptions(3u, MetricKind.Cos, ScalarKind.F32)
    private val exampleIndex: Index
        get() {
            val index = Index(exampleOpts)
            val vec = floatArrayOf(3.7f, 4.9f, -36f)
            (0..10).forEach {
                index.add(it.toULong(), vec)
            }
            return index
        }

    @Test
    fun initialize() {
        val options = IndexOptions(256u, MetricKind.Cos, ScalarKind.F32)
        val index = Index(options)
        println("Hardware acceleration: ${index.hardwareAcceleration}")
        assertEquals(MetricKind.Cos, index.metricKind)
        assertEquals(64u, index.expansionSearch)
        assertEquals(128u, index.expansionAdd)
        assertEquals(256u, index.dimensions)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun search() {
        val index = Index(exampleOpts)
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(1.1f, 2f, 3f)
        index.asF32.add(1u, a)
        index.asF32.add(2u, b)
        val matches = index.search(a, 2)
        assertContentEquals(ulongArrayOf(1u, 2u), matches.keys.sorted())
    }

    @Test
    fun saveBuffer() {
        val index = exampleIndex
        val buffer = ByteArray(4 shl 10)
        index.saveBuffer(buffer)

        val load = Index(exampleOpts)
        load.loadBuffer(buffer)
        assertEquals(index.size, load.size)
    }

    @Test
    fun saveEmptyBuffer() {
        val index = exampleIndex
        val buffer = ByteArray(0)
        assertFailsWith(IllegalArgumentException::class) {
            index.saveBuffer(buffer)
        }
    }

    @Test
    fun indexOutOfBounds() {
        val index = Index(exampleOpts)
        val a = floatArrayOf(1f, 2f, 3f)
        index.asF32.add(0u, a)

        val matches = index.search(a, 1)
        assertFailsWith(IndexOutOfBoundsException::class) {
            matches.keys[1]
        }
        assertFailsWith(IndexOutOfBoundsException::class) {
            matches.distances[1]
        }
    }

    @Test
    fun get() {
        val index = Index(exampleOpts)
        index.asF64.add(0u, DoubleArray(3) { PI })
        assertContentEquals(FloatArray(3) { PI.toFloat() }, index.asF32[0u])
        assertContentEquals(Float16Array(3) { PI.toFloat16() }, index.asF16[0u])
        assertNull(index.asF32[1u])
    }

    @Test
    fun addF16() {
        val index = Index(exampleOpts)
        val a = Float16Array(3) { E.toFloat16() }
        index.asF16.add(0u, a)
        assertContentEquals(a, index.asF16[0u])
    }

    @Test
    fun addI8() {
        val index = Index(exampleOpts)
        val a = byteArrayOf(-18, 127, 0)
        index.asI8.add(0u, a)
        assertContentEquals(a, index.asI8[0u])
    }
}
