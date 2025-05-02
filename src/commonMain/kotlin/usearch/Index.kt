package usearch

expect class Index(options: IndexOptions) {
    var expansionAdd: ULong
    var expansionSearch: ULong
    var metricKind: MetricKind
    val dimensions: ULong

    val size: ULong
    val capacity: ULong
    val hardwareAcceleration: String?

    @Deprecated("Use explicit add instead", ReplaceWith("asF32.add"))
    fun add(key: ULong, f32Vector: FloatArray)

    @Deprecated("Use explicit add instead", ReplaceWith("asF64.add"))
    fun add(key: ULong, f64Vector: DoubleArray)

    fun remove(key: ULong)

    val asF32: IndexQuery<FloatArray>
    val asF64: IndexQuery<DoubleArray>
    val asF16: IndexQuery<Float16Array>
    val asI8: IndexQuery<ByteArray>
    val asB1x8: IndexQuery<ByteArray>

    fun search(query: FloatArray, count: Int): Matches

    fun loadFile(filePath: String)
    fun loadBuffer(buffer: ByteArray)
    fun saveFile(filePath: String)
    fun saveBuffer(buffer: ByteArray)

    companion object {
        val INITIAL_CAPACITY: Long
    }
}