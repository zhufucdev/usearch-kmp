package usearch

interface IndexQuery<T> {
    /**
     * Adds a vector with a key to the index.
     * @param key the key associated with the vector.
     * @param vec the vector data to add.
     * @throws USearchException when [IndexOptions.multi] is off and key already exists.
     */
    fun add(key: ULong, vec: T)

    /**
     * Retrieves the vector associated with the given key from the index.
     * @param key the key of the vector to retrieve.
     */
    operator fun get(key: ULong): T?

    /**
     * Retrieves the vector associated with the given key from the index.
     * @param key the key of the vector to retrieve.
     * @param count upper bound of result amount.
     */
    operator fun get(key: ULong, count: ULong): List<T>
}
