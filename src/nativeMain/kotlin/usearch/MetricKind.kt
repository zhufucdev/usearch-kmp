package usearch

import kotlinx.cinterop.ExperimentalForeignApi
import lib.*

@OptIn(ExperimentalForeignApi::class)
actual enum class MetricKind(val nativeEnum: UInt) {
    IP(usearch_metric_ip_k), L2sq(usearch_metric_l2sq_k), Cos(usearch_metric_cos_k),
    Pearson(usearch_metric_pearson_k), Haversine(usearch_metric_haversine_k), Divergence(usearch_metric_divergence_k),
    Hamming(usearch_metric_hamming_k), Tanimoto(usearch_metric_tanimoto_k), Sorensen(usearch_metric_sorensen_k)
}
