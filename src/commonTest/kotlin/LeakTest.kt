import kotlinx.coroutines.*
import usearch.*
import kotlin.test.Test
import kotlin.test.assertContentEquals

class LeakTest {
    @Test
    fun testMatch() = runBlocking {
        repeat(100) {
            launch {
                val index = Index(IndexOptions(512u, MetricKind.Cos, ScalarKind.F16))
                for (i in 0 until 72) {
                    index.asF16.add(i.toULong(), Float16Array(512) { Float16((it + i).toShort()) })
                }
                val vectors = getNiceVectors(index).toMutableSet()
                while (true) {
                    val searchResult = vectors
                        .map {
                            async(Dispatchers.Default) {
                                val value = index.asF32[it.key.toULong()]!!
                                index.search(value, 3)
                                    .map { it.toNiceVector() }
                            }
                        }
                        .awaitAll()
                        .flatten()

                    vectors.addAll(searchResult)
                    if (vectors.size.toULong() == index.size) {
                        break
                    }
                }
            }
        }
    }

    @Test
    fun testSave() {
        val index = Index(IndexOptions(512u, MetricKind.Cos, ScalarKind.F16))
        for (i in 0 until 72) {
            index.asF32.add(i.toULong(), FloatArray(512) { (it + i).toFloat() })
        }
        val ba = ByteArray(index.serializedLength.toInt())
        index.saveBuffer(ba)
        val loadedIndex = Index(IndexOptions(512u, MetricKind.Cos, ScalarKind.F16)).apply {
            loadBuffer(ba)
        }
        runBlocking {
            for (i in 0 until 72) {
                val query = FloatArray(512) { (it + i).toFloat() }
                assertContentEquals(index.search(query, 10), loadedIndex.search(query, 10))
            }
        }
    }
}

fun Match.toNiceVector() = NiceVector(key.toLong(), 512)

fun getNiceVectors(index: Index): List<NiceVector> {
    return index.search(FloatArray(512) { Float16(it.toShort()).toFloat() }, 10)
        .map { it.toNiceVector() }
}

data class NiceVector(val key: Long, val dimension: Long)

