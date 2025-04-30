import usearch.Index
import usearch.IndexOptions
import usearch.MetricKind
import usearch.ScalarKind
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IndexTest {
    private val exampleOpts: IndexOptions get() = IndexOptions(3u, MetricKind.Cos, ScalarKind.F32)
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
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun search() {
        val index = Index(exampleOpts)
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(1.1f, 2f, 3f)
        index.add(1u, a)
        index.add(2u, b)
        val matches = index.search(a, 1)
        assertContentEquals(ulongArrayOf(2u), matches.keys)
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
    fun indexOutOfBounds() {
        val index = Index(exampleOpts)
        val a = floatArrayOf(1f, 2f, 3f)
        index.add(0u, a)

        val matches = index.search(a, 1)
        assertFailsWith(IndexOutOfBoundsException::class) {
            matches.keys[1]
        }
        assertFailsWith(IndexOutOfBoundsException::class) {
            matches.distances[1]
        }
    }
}
