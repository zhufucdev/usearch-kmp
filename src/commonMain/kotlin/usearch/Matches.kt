package usearch

@OptIn(ExperimentalUnsignedTypes::class)
expect class Matches : Iterable<Match> {
    val keys: List<ULong>
    val distances: List<Float>
    override fun iterator(): Iterator<Match>
}

data class Match(val key: ULong, val distance: Float)