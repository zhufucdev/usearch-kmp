package usearch

@OptIn(ExperimentalUnsignedTypes::class)
actual data class Matches(
    actual val keys: List<ULong>,
    actual val distances: List<Float>
) : Iterable<Match> {
    actual override fun iterator(): Iterator<Match> = iterator {
        for (i in 0 until keys.size) {
            yield(Match(keys[i], distances[i]))
        }
    }
}
