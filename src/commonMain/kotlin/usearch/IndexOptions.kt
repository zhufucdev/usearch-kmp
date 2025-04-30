package usearch

data class IndexOptions(
    val dimensions: ULong,
    val metric: MetricKind,
    val quantization: ScalarKind,
    val connectivity: ULong = 0u,
    val expansionAdd: ULong = 128u,
    val expansionSearch: ULong = 64u,
    val multi: Boolean = false
)
