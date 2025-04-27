package usearch

data class IndexOptions(
    val dimensions: ULong,
    val metric: MetricKind,
    val quantization: ScalarKind,
    val connectivity: ULong = 0u,
    val expansionAdd: ULong = 0u,
    val expansionSearch: ULong = 0u,
    val multi: Boolean = false
)
