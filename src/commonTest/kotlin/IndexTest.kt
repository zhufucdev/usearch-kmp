import usearch.Index
import usearch.IndexOptions
import usearch.MetricKind
import usearch.ScalarKind
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class IndexTest {
    @Test
    fun initialize() {
        val options = IndexOptions(256u, MetricKind.Cos, ScalarKind.F32)
        val index = Index(options)
        assertEquals(MetricKind.Cos, index.metricKind)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun search() {
        val index = Index(IndexOptions(3u, MetricKind.Cos, ScalarKind.F16))
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(1.1f, 2f, 3f)
        index.add(1u, a)
        index.add(2u, b)
        val matches = index.search(a, 1)
        assertContentEquals(ulongArrayOf(2u), matches.keys)
    }
}
