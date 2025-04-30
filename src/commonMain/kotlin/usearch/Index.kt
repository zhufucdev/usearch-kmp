package usearch

expect class Index(options: IndexOptions) {
    var expansionAdd: ULong
    var expansionSearch: ULong
    var metricKind: MetricKind

    val size: ULong
    val capacity: ULong
    val hardwareAcceleration: String?

    fun add(key: ULong, f32Vector: FloatArray)
    fun add(key: ULong, f64Vector: DoubleArray)
    fun search(query: FloatArray, count: Int): Matches

    fun loadFile(filePath: String)
    fun loadBuffer(buffer: ByteArray)
    fun saveFile(filePath: String)
    fun saveBuffer(buffer: ByteArray)

    companion object {
        val INITIAL_CAPACITY: Long
    }
}