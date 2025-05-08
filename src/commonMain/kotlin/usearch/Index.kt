package usearch

expect class Index(options: IndexOptions) {
    /**
     * The expansion factor used for index construction when adding vectors.
     */
    var expansionAdd: ULong

    /**
     * The expansion factor used for index construction during search operations.
     */
    var expansionSearch: ULong

    /**
     * The metric kind used for distance calculation between vectors.
     */
    var metricKind: MetricKind

    /**
     * Reports the current dimensions of the vectors in the index.
     */
    val dimensions: ULong

    /**
     * Reports the current size (number of vectors) of the index.
     */
    val size: ULong

    /**
     * Reports the current capacity (number of vectors) of the index.
     */
    val capacity: ULong

    /**
     * Reports the memory usage of the index in bytes.
     */
    val memoryUsage: ULong

    /**
     * Reports the SIMD capabilities used by the index on the current CPU.
     */
    val hardwareAcceleration: String?

    /**
     * Reports the expected file size after serialization.
     */
    val serializedLength: ULong

    @Deprecated("Use explicit add instead", ReplaceWith("asF32.add"))
    fun add(key: ULong, f32Vector: FloatArray)

    @Deprecated("Use explicit add instead", ReplaceWith("asF64.add"))
    fun add(key: ULong, f64Vector: DoubleArray)

    /**
     * Removes the vector associated with the given key from the index.
     * @param key The key of the vector to be removed.
     */
    fun remove(key: ULong)

    /**
     * Reserves memory for a specified number of incoming vectors.
     */
    fun reserve(capacity: ULong)

    /**
     * CRUD in single precision floating point mode.
     */
    val asF32: IndexQuery<FloatArray>

    /**
     * CRUD in double precision floating point mode.
     */
    val asF64: IndexQuery<DoubleArray>

    /**
     * CRUD in half precision floating point mode.
     */
    val asF16: IndexQuery<Float16Array>

    /**
     * CRUD in 8-bit integer mode.
     */
    val asI8: IndexQuery<ByteArray>

    /**
     * CRUD in 1-bit binary mode.
     */
    val asB1x8: IndexQuery<ByteArray>

    /**
     * Performs k-Approximate Nearest Neighbors (kANN) Search for closest vectors to query.
     * @param query query vector, which to search around.
     * @param count upper bound on the number of neighbors to search, the "k" in "kANN".
     */
    fun search(query: FloatArray, count: Int): Matches

    /**
     * Loads the index from a file.
     * @param filePath path of the file to load.
     */
    fun loadFile(filePath: String)

    /**
     * Loads the index from an in-memory buffer.
     * @param buffer the buffer to load.
     */
    fun loadBuffer(buffer: ByteArray)

    /**
     * Saves the index to a file.
     * @param filePath path of the file to load.
     */
    fun saveFile(filePath: String)

    /**
     * Saves the index to an in-memory buffer.
     * @param buffer the buffer to save to.
     */
    fun saveBuffer(buffer: ByteArray)

    companion object {
        /**
         * Default memory reservation amount of the index.
         */
        val INITIAL_CAPACITY: Long

        /**
         * Default memory allocation when incoming insertion fails with
         * inadequate capacity.
         */
        val INCREMENTAL_CAPACITY: Long
    }
}