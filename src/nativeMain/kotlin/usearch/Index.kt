package usearch

import kotlinx.cinterop.*
import lib.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
actual class Index {
    private val inner: StableRef<CPointed>
    private var _metricKind: MetricKind

    private val cleaner: Cleaner

    actual constructor(options: IndexOptions) {
        val opts = options.native()
        errorScoped {
            inner = usearch_init(opts.getPointer(this), err)?.asStableRef() ?: error("No error returned while init ptr is null.")
            _metricKind = options.metric
        }
        errorScoped {
            // somehow the C implementation doesn't reserve at init time
            usearch_reserve(inner.asCPointer(), INITIAL_CAPACITY.toULong(), err)
        }

        cleaner = createCleaner(inner) {
            try {
                errorScoped {
                    usearch_free(it.asCPointer(), err)
                }
            } catch (e: IllegalStateException) {
                println("Error calling usearch_free: ${e.message}")
            }
        }
    }

    actual var expansionAdd: ULong
        get() = errorScoped {
            usearch_expansion_add(inner.asCPointer(), err)
        }
        set(value) {
            errorScoped {
                usearch_change_expansion_add(inner.asCPointer(), value, err)
            }
        }

    actual var expansionSearch: ULong
        get() = errorScoped {
            usearch_expansion_search(inner.asCPointer(), err)
        }
        set(value) {
            errorScoped {
                usearch_change_expansion_search(inner.asCPointer(), value, err)
            }
        }

    actual var metricKind: MetricKind
        get() = _metricKind
        set(value) {
            errorScoped {
                usearch_change_metric_kind(inner.asCPointer(), value.nativeEnum, err)
            }
            _metricKind = value
        }

    actual val hardwareAcceleration: String?
        get() = errorScoped {
            usearch_hardware_acceleration(inner.asCPointer(), err)?.toKString()
        }

    actual fun add(key: ULong, f32Vector: FloatArray) {
        val vector = f32Vector.toCValues()
        errorScoped {
            usearch_add(inner.asCPointer(), key, vector, usearch_scalar_f32_k, err)
        }
    }

    actual fun add(key: ULong, f64Vector: DoubleArray) {
        val vector = f64Vector.toCValues()
        errorScoped {
            usearch_add(inner.asCPointer(), key, vector, usearch_scalar_f64_k, err)
        }
    }

    actual fun search(query: FloatArray, count: Int): Matches {
        val queryArr = query.toCValues()
        val keys = nativeHeap.allocArray<usearch_key_tVar>(count)
        val distances = nativeHeap.allocArray<FloatVar>(count)
        return errorScoped {
            val count = usearch_search(
                inner.asCPointer(),
                queryArr,
                usearch_scalar_f32_k,
                count.toULong(),
                keys,
                distances,
                err
            )
            Matches(
                keys = object : DelegatedList<usearch_key_t>(count.toInt()) {
                    override fun get(index: Int): usearch_key_t = keys[index]
                    val cleaner = createCleaner(keys) {
                        nativeHeap.free(it)
                    }
                },
                distances = object : DelegatedList<Float>(count.toInt()) {
                    override fun get(index: Int): Float = distances[index]
                    val cleaner = createCleaner(distances) {
                        nativeHeap.free(it)
                    }
                },
            )
        }
    }

    actual companion object {
        actual val INITIAL_CAPACITY: Long = 5L
    }
}