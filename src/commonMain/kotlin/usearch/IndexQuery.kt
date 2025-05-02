package usearch

interface IndexQuery<T> {
    fun add(key: ULong, vec: T)
    operator fun get(key: ULong): T?
    operator fun get(key: ULong, count: ULong): List<T>
}

