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
            } finally {
                it.dispose()
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
        asF64.add(key, f64Vector)
    }

    actual fun remove(key: ULong) {
        errorScoped {
            usearch_remove(inner.asCPointer(), key, err)
        }
    }

    actual fun reserve(capacity: ULong) {
        errorScoped {
            usearch_reserve(inner.asCPointer(), capacity, err)
        }
    }

    actual val asF32: IndexQuery<FloatArray> by lazy(::F32Q)
    actual val asF64: IndexQuery<DoubleArray> by lazy(::F64Q)
    actual val asF16: IndexQuery<Float16Array> by lazy(::F16Q)
    actual val asI8: IndexQuery<ByteArray> by lazy(::I8Q)
    actual val asB1x8: IndexQuery<ByteArray> by lazy(::B1Q)

    actual fun search(query: FloatArray, count: Int): Matches {
        return errorScoped {
            val keys = allocArray<usearch_key_tVar>(count)
            val distances = allocArray<FloatVar>(count)
            val size = query.usePinned {
                usearch_search(
                    inner.asCPointer(),
                    it.addressOf(0),
                    usearch_scalar_f32_k,
                    count.toULong(),
                    keys,
                    distances,
                    err
                )
            }.toInt()
            Matches(List(size) { keys[it] }, List(size) { distances[it] })
        }
    }

    actual fun loadFile(filePath: String) {
        errorScoped {
            usearch_load(inner.asCPointer(), filePath, err)
        }
    }

    actual fun loadBuffer(buffer: ByteArray) {
        if (buffer.isEmpty()) {
            throw IllegalArgumentException("Cannot load empty buffer.")
        }
        errorScoped {
            buffer.usePinned {
                usearch_load_buffer(
                    index = inner.asCPointer(),
                    buffer = it.addressOf(0),
                    length = buffer.size.toULong(),
                    error = err
                )
            }
        }
    }

    actual fun saveFile(filePath: String) {
        errorScoped {
            usearch_save(inner.asCPointer(), filePath, err)
        }
    }

    actual fun saveBuffer(buffer: ByteArray) {
        if (buffer.isEmpty()) {
            throw IllegalArgumentException("Cannot save to empty buffer.")
        }
        errorScoped {
            buffer.usePinned {
                usearch_save_buffer(
                    index = inner.asCPointer(),
                    buffer = it.addressOf(0),
                    length = buffer.size.toULong(),
                    error = err
                )
            }
        }
    }

    abstract inner class CommonIndexQuery<T : Any, C : CPrimitiveVar>(val vectorKind: ScalarKind) : IndexQuery<T> {
        abstract fun AutofreeScope.constructDefaultArray(size: Int): CPointer<C>
        abstract fun MemScope.ptr(vec: T): CPointer<C>
        abstract fun T.slice(indices: IntRange): T
        abstract fun isEmpty(vec: T): Boolean
        abstract fun CPointer<C>.toKt(size: Int): T

        override fun add(key: ULong, vec: T) {
            if (isEmpty(vec)) {
                throw IllegalArgumentException("Cannot add empty vector.")
            }
            if (capacity < size + 1u) {
                reserve(INCREMENTAL_CAPACITY.toULong())
            }
            errorScoped {
                usearch_add(inner.asCPointer(), key, memScope.ptr(vec), vectorKind.nativeEnum, err)
            }
        }

        override fun get(key: ULong): T? = errorScoped {
            val buf = constructDefaultArray(dimensions.toInt())
            val actualCount = usearch_get(inner.asCPointer(), key, 1u, buf, vectorKind.nativeEnum, err)
            if (actualCount < 1u) {
                return@errorScoped null
            } else {
                buf.toKt(dimensions.toInt())
            }
        }

        override fun get(key: ULong, count: ULong): List<T> = errorScoped {
            val dimensions = dimensions.toInt()
            val buf = constructDefaultArray(dimensions * count.toInt())
            val count = usearch_get(inner.asCPointer(), key, count, buf, usearch_scalar_f32_k, err)
            buf.toKt(count.toInt() * dimensions).let {
                (0 until count.toInt()).map { part ->
                    it.slice(part * dimensions until (part + 1) * dimensions)
                }
            }
        }
    }

    inner class F32Q : CommonIndexQuery<FloatArray, FloatVar>(ScalarKind.F32) {
        override fun AutofreeScope.constructDefaultArray(size: Int): CPointer<FloatVar> = allocArray(size)
        override fun MemScope.ptr(vec: FloatArray): CPointer<FloatVar> = vec.toCValues().ptr
        override fun FloatArray.slice(indices: IntRange): FloatArray = sliceArray(indices)
        override fun isEmpty(vec: FloatArray): Boolean = vec.isEmpty()
        override fun CPointer<FloatVar>.toKt(size: Int): FloatArray = FloatArray(size, ::get)
    }

    inner class F64Q : CommonIndexQuery<DoubleArray, DoubleVar>(ScalarKind.F64) {
        override fun isEmpty(vec: DoubleArray): Boolean = vec.isEmpty()
        override fun CPointer<DoubleVar>.toKt(size: Int): DoubleArray = DoubleArray(size, ::get)
        override fun AutofreeScope.constructDefaultArray(size: Int): CPointer<DoubleVar> = allocArray(size)
        override fun MemScope.ptr(vec: DoubleArray): CPointer<DoubleVar> = vec.toCValues().ptr
        override fun DoubleArray.slice(indices: IntRange): DoubleArray = sliceArray(this@slice.indices)
    }

    inner class F16Q : CommonIndexQuery<Float16Array, ShortVar>(ScalarKind.F16) {
        override fun isEmpty(vec: Float16Array): Boolean = vec.isEmpty()
        override fun CPointer<ShortVar>.toKt(size: Int): Float16Array = Float16Array(size) { Float16(get(it)) }
        override fun AutofreeScope.constructDefaultArray(size: Int): CPointer<ShortVar> = allocArray(size)
        override fun MemScope.ptr(vec: Float16Array): CPointer<ShortVar> = vec.toRawBits().toCValues().ptr
        override fun Float16Array.slice(indices: IntRange): Float16Array = sliceArray(indices)
    }

    inner class I8Q : CommonIndexQuery<ByteArray, ByteVar>(ScalarKind.I8) {
        override fun isEmpty(vec: ByteArray): Boolean = vec.isEmpty()
        override fun CPointer<ByteVar>.toKt(size: Int): ByteArray = ByteArray(size, ::get)
        override fun AutofreeScope.constructDefaultArray(size: Int): CPointer<ByteVar> = allocArray(size)
        override fun MemScope.ptr(vec: ByteArray): CPointer<ByteVar> = vec.toCValues().ptr
        override fun ByteArray.slice(indices: IntRange): ByteArray = sliceArray(indices)
    }

    inner class B1Q : CommonIndexQuery<ByteArray, ByteVar>(ScalarKind.B1) {
        override fun isEmpty(vec: ByteArray): Boolean = vec.isEmpty()
        override fun CPointer<ByteVar>.toKt(size: Int): ByteArray = ByteArray(size, ::get)
        override fun AutofreeScope.constructDefaultArray(size: Int): CPointer<ByteVar> = allocArray(size)
        override fun MemScope.ptr(vec: ByteArray): CPointer<ByteVar> = vec.toCValues().ptr
        override fun ByteArray.slice(indices: IntRange): ByteArray = sliceArray(indices)
    }

    actual companion object {
        actual val INITIAL_CAPACITY: Long = 5L
        actual val INCREMENTAL_CAPACITY: Long = 5L
    }
}