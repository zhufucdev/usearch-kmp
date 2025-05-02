package usearch

/**
 * The index options used to configure the dense index during creation.
 * It contains the number of dimensions, the metric kind, the scalar kind, the connectivity,
 * the expansion values, and the multi-flag.
 */
data class IndexOptions(
    /**
     *  The number of dimensions in the vectors to be indexed.
     *  Must be defined for most metrics, but can be avoided for `usearch_metric_haversine_k`.
     */
    val dimensions: ULong,

    /**
     *  The metric kind used for distance calculation between vectors.
     */
    val metric: MetricKind,

    /**
     *  The scalar kind used for quantization of vector data during indexing.
     *  In most cases, on modern hardware, it's recommended to use half-precision floating-point numbers.
     *  When quantization is enabled, the "get"-like functions won't be able to recover the original data,
     *  so you may want to replicate the original vectors elsewhere.
     *
     *  Quantizing to integers is also possible, but it's important to note that it's only valid for cosine-like
     *  metrics. As part of the quantization process, the vectors are normalized to unit length and later scaled
     *  to `[-127,127]` range to occupy the full 8-bit range.
     *
     *  Quantizing to 1-bit booleans is also possible, but it's only valid for binary metrics like Jaccard, Hamming,
     *  etc. As part of the quantization process, the scalar components greater than zero are set to `true`, and the
     *  rest to `false`.
     */
    val quantization: ScalarKind,

    /**
     *  The optional connectivity parameter that limits connections-per-node in graph.
     */
    val connectivity: ULong = 0u,

    /**
     * The optional expansion factor used for index construction when adding vectors.
     */
    val expansionAdd: ULong = 128u,

    /**
     *  The optional expansion factor used for index construction during search operations.
     */
    val expansionSearch: ULong = 64u,

    /**
     *  When set allows multiple vectors to map to the same key.
     */
    val multi: Boolean = false
)
