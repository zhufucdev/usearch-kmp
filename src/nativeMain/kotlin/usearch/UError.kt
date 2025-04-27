@file:OptIn(ExperimentalForeignApi::class)

package usearch

import kotlinx.cinterop.*
import lib.usearch_error_tVar
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class ErrorScope : AutofreeScope() {
    abstract val err: CPointer<usearch_error_tVar>
}

@OptIn(ExperimentalContracts::class)
inline fun <T> errorScoped(block: (ErrorScope.() -> T)): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    memScoped {
        val err = alloc<usearch_error_tVar>()
        val res = block(object : ErrorScope() {
            override val err: CPointer<usearch_error_tVar>
                get() = err.ptr

            override fun alloc(size: Long, align: Int): NativePointed = this@memScoped.alloc(size, align)
        })
        err.value?.toKString()?.let(::error)
        return res
    }
}