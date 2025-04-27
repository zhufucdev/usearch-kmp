package usearch

@OptIn(ExperimentalUnsignedTypes::class)
expect class Matches {
    val keys: List<ULong>
    val distances: List<Float>
}