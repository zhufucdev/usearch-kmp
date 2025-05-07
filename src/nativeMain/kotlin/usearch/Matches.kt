package usearch

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalForeignApi::class)
actual data class Matches(
    actual val keys: List<ULong>,
    actual val distances: List<Float>
) : Iterable<Match> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Matches

        if (keys != other.keys) return false
        if (distances != other.distances) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keys.hashCode()
        result = 31 * result + distances.hashCode()
        return result
    }

    actual override fun iterator(): Iterator<Match> = iterator {
        for (i in 0 until keys.size) {
            yield(Match(keys[i], distances[i]))
        }
    }
}
