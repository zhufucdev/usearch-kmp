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
            inner = usearch_init(opts.getPointer(this), err)?.asStableRef()
                ?: error("No error returned while init ptr is null.")
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

    actual val dimensions: ULong
        get() = errorScoped {
            usearch_dimensions(inner.asCPointer(), err)
        }

    actual val size: ULong
        get() = errorScoped {
            usearch_size(inner.asCPointer(), err)
        }

    actual val capacity: ULong
        get() = errorScoped {
            usearch_capacity(inner.asCPointer(), err)
        }

    actual val hardwareAcceleration: String?
        get() = errorScoped {
            usearch_hardware_acceleration(inner.asCPointer(), err)?.toKString()
        }

    actual val memoryUsage: ULong
        get() = errorScoped {
            usearch_memory_usage(inner.asCPointer(), err)
        }

    actual fun add(key: ULong, f32Vector: FloatArray) {
        asF32.add(key, f32Vector)
    }

    actual fun add(key: ULong, f64Vector: DoubleArray) {
        errorScoped {
            f64Vector.usePinned {
                usearch_add(inner.asCPointer(), key, it.addressOf(0), usearch_scalar_f64_k, err)
            }
        }
    }

    actual fun remove(key: ULong) {
        errorScoped {
            usearch_remove(inner.asCPointer(), key, err)
        }
    }

    actual val asF32: IndexQuery<FloatArray> by lazy(::F32Q)
    actual val asF64: IndexQuery<DoubleArray> by lazy(::F64Q)
    actual val asF16: IndexQuery<Float16Array> by lazy(::F16Q)
    actual val asI8: IndexQuery<ByteArray> by lazy(::I8Q)
    actual val asB1x8: IndexQuery<ByteArray> by lazy(::B1Q)

    actual fun search(query: FloatArray, count: Int): Matches {
        val keys = nativeHeap.allocArray<usearch_key_tVar>(count)
        val distances = nativeHeap.allocArray<FloatVar>(count)
        return errorScoped {
            val count = query.usePinned {
                usearch_search(
                    inner.asCPointer(),
                    it.addressOf(0),
                    usearch_scalar_f32_k,
                    count.toULong(),
                    keys,
                    distances,
                    err
                )
            }
            Matches(
                keys = object : DelegatedList<usearch_key_t>(count.toInt()) {
                    override fun get(index: Int): usearch_key_t {
                        if (index >= count.toInt()) {
                            throw IndexOutOfBoundsException("index $index is out of bounds")
                        }
                        return keys[index]
                    }

                    val cleaner = createCleaner(keys) {
                        nativeHeap.free(it)
                    }
                },
                distances = object : DelegatedList<Float>(count.toInt()) {
                    override fun get(index: Int): Float {
                        if (index >= count.toInt()) {
                            throw IndexOutOfBoundsException("index $index is out of bounds")
                        }
                        return distances[index]
                    }

                    val cleaner = createCleaner(distances) {
                        nativeHeap.free(it)
                    }
                },
            )
        }
    }

    actual fun loadFile(filePath: String) {
        errorScoped {
            usearch_load(inner.asCPointer(), filePath, err)
        }
    }

    actual fun loadBuffer(buffer: ByteArray) {
        errorScoped {
            buffer.usePinned {
                usearch_load_buffer(inner.asCPointer(), it.addressOf(0), buffer.size.toULong(), err)
            }
        }
    }

    actual fun saveFile(filePath: String) {
        errorScoped {
            usearch_save(inner.asCPointer(), filePath, err)
        }
    }

    actual fun saveBuffer(buffer: ByteArray) {
        errorScoped {
            buffer.usePinned {
                usearch_save_buffer(inner.asCPointer(), it.addressOf(0), buffer.size.toULong(), err)
            }
        }
    }

    abstract inner class CommonIndexQuery<T : Any>(val vectorKind: ScalarKind) : IndexQuery<T> {
        abstract fun constructEmptyArray(count: Int): T
        abstract fun Pinned<T>.addr(index: Int): CPointer<*>
        abstract fun T.slice(indices: IntRange): T

        override fun add(key: ULong, vec: T) {
            errorScoped {
                vec.usePinned {
                    usearch_add(inner.asCPointer(), key, it.addr(0), vectorKind.nativeEnum, err)
                }
            }
        }

        override fun get(key: ULong): T? = errorScoped {
            constructEmptyArray(dimensions.toInt()).apply {
                usePinned {
                    val actualCount = usearch_get(inner.asCPointer(), key, 1u, it.addr(0), vectorKind.nativeEnum, err)
                    if (actualCount < 1u) {
                        return@errorScoped null
                    }
                }
            }
        }

        override fun get(key: ULong, count: ULong): List<T> = errorScoped {
            val dimensions = dimensions.toInt()
            constructEmptyArray(dimensions * count.toInt()).apply {
                usePinned {
                    usearch_get(inner.asCPointer(), key, count, it.addr(0), usearch_scalar_f32_k, err)
                }
            }.let {
                (0 until count.toInt()).map { part ->
                    it.slice(part * dimensions until (part + 1) * dimensions)
                }
            }
        }
    }

    inner class F32Q : CommonIndexQuery<FloatArray>(ScalarKind.F32) {
        override fun constructEmptyArray(count: Int): FloatArray = FloatArray(count)

        override fun Pinned<FloatArray>.addr(index: Int): CPointer<*> = addressOf(index)

        override fun FloatArray.slice(indices: IntRange): FloatArray = sliceArray(indices)
    }

    inner class F64Q : CommonIndexQuery<DoubleArray>(ScalarKind.F64) {
        override fun constructEmptyArray(count: Int): DoubleArray = DoubleArray(count)

        override fun Pinned<DoubleArray>.addr(index: Int): CPointer<*> = addressOf(index)

        override fun DoubleArray.slice(indices: IntRange): DoubleArray = sliceArray(this@slice.indices)
    }

    inner class F16Q : CommonIndexQuery<Float16Array>(ScalarKind.F16) {
        override fun constructEmptyArray(count: Int): Float16Array = Float16Array(count)

        override fun Pinned<Float16Array>.addr(index: Int): CPointer<*> = get().inner.usePinned { it.addressOf(index) }

        override fun Float16Array.slice(indices: IntRange): Float16Array = sliceArray(indices)
    }

    inner class I8Q : CommonIndexQuery<ByteArray>(ScalarKind.I8) {
        override fun constructEmptyArray(count: Int): ByteArray = ByteArray(count)

        override fun Pinned<ByteArray>.addr(index: Int): CPointer<*> = addressOf(index)

        override fun ByteArray.slice(indices: IntRange): ByteArray = sliceArray(indices)
    }

    inner class B1Q : CommonIndexQuery<ByteArray>(ScalarKind.B1) {
        override fun constructEmptyArray(count: Int): ByteArray = ByteArray(count)

        override fun Pinned<ByteArray>.addr(index: Int): CPointer<*> = addressOf(index)

        override fun ByteArray.slice(indices: IntRange): ByteArray = sliceArray(indices)
    }

    actual companion object {
        actual val INITIAL_CAPACITY: Long = 5L
    }
}