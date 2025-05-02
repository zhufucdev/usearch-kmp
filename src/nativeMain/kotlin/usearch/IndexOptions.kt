@file:OptIn(ExperimentalForeignApi::class)

package usearch

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import lib.usearch_init_options_t

fun IndexOptions.native(): CValue<usearch_init_options_t> = cValue {
    dimensions = this@native.dimensions
    connectivity = this@native.connectivity
    metric_kind = this@native.metric.nativeEnum
    quantization = this@native.quantization.nativeEnum
    connectivity = this@native.connectivity
    expansion_add = this@native.expansionAdd
    expansion_search = this@native.expansionSearch
    multi = this@native.multi
}