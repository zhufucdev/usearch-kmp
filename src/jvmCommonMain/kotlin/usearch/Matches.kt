package usearch

@OptIn(ExperimentalUnsignedTypes::class)
actual class Matches internal constructor(private val ptr: Long) {
    private val size = NativeMethods.bridge.usearch_sresult_size(ptr)

    actual val keys: List<ULong> = object : DelegatedList<ULong>(size) {
        override fun get(index: Int): ULong = NativeMethods.bridge.usearch_sresult_key_at(ptr, index).toULong()
    }

    actual val distances: List<Float> = object : DelegatedList<Float>(size) {
        override fun get(index: Int): Float = NativeMethods.bridge.usearch_sresult_distance_at(ptr, index)
    }
}
