package usearch

actual class Index(
    private val ptr: Long,
    private var _metricKind: MetricKind
) {
    actual constructor(options: IndexOptions) : this({
        val opts = NativeMethods.bridge.usearch_new_index_opts(
            options.dimensions.toLong(),
            options.metric.nativeEnum,
            options.quantization.nativeEnum,
            options.connectivity.toLong(),
            options.expansionAdd.toLong(),
            options.expansionSearch.toLong(),
            options.multi
        )
        try {
            val ptr = NativeMethods.bridge.usearch_init(opts)
            NativeMethods.bridge.usearch_reserve(ptr, INITIAL_CAPACITY)
            ptr
        } finally {
            NativeMethods.bridge.release_index_opts(opts)
        }
    }(), options.metric)

    actual var expansionAdd: ULong
        get() = NativeMethods.bridge.usearch_expansion_add(ptr).toULong()
        set(value) {
            NativeMethods.bridge.usearch_change_expansion_add(ptr, value.toLong())
        }

    actual var expansionSearch: ULong
        get() = NativeMethods.bridge.usearch_expansion_search(ptr).toULong()
        set(value) {
            NativeMethods.bridge.usearch_change_expansion_search(ptr, value.toLong())
        }

    actual var metricKind: MetricKind
        get() = _metricKind
        set(value) {
            _metricKind = value
        }

    actual val hardwareAcceleration: String?
        get() = NativeMethods.bridge.usearch_hardware_acceleration(ptr)

    actual fun add(key: ULong, f32Vector: FloatArray) {
        NativeMethods.bridge.usearch_add_f32(ptr, key.toLong(), f32Vector)
    }

    actual fun add(key: ULong, f64Vector: DoubleArray) {
        NativeMethods.bridge.usearch_add_f64(ptr, key.toLong(), f64Vector)
    }

    actual fun search(query: FloatArray, count: Int): Matches {
        val p = NativeMethods.bridge.usearch_search(ptr, query, count)
        return Matches(p)
    }

    protected fun finalize() {
        NativeMethods.bridge.usearch_free(ptr)
    }

    actual companion object {
        actual val INITIAL_CAPACITY: Long = 5L
    }
}