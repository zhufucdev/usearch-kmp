package usearch

expect class Index(options: IndexOptions) {
    var expansionAdd: ULong
    var expansionSearch: ULong
    var metricKind: MetricKind

    val hardwareAcceleration: String?

    fun add(key: ULong, f32Vector: FloatArray)
    fun add(key: ULong, f64Vector: DoubleArray)
    fun search(query: FloatArray, count: Int): Matches

    companion object {
        val INITIAL_CAPACITY: Long
    }
}