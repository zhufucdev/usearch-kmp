@file:OptIn(ExperimentalForeignApi::class)

package usearch

import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import lib.usearch_index_tVar
import lib.usearch_free

fun <T> CPointer<ByteVarOf<Byte>>.use(index: CPointer<usearch_index_tVar>, block: String.() -> T): T {
    return try {
        block(toKString())
    } finally {
        usearch_free(index, null)
    }
}
