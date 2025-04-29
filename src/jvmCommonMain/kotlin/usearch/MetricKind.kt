package usearch

actual enum class MetricKind(val nativeEnum: Int) {
    IP(2), L2sq(3), Cos(1), Pearson(6),
    Haversine(4), Divergence(5), Hamming(8),
    Tanimoto(9), Sorensen(10)
}
