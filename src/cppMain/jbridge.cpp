#include <iostream>
#include <usearch/index_dense.hpp>

#include "jexceptions.h"

extern "C" {
#include "lib.h"
#include "usearch_NativeBridge.h"
}

using search_result_t = unum::usearch::index_dense_gt<>::search_result_t;

template<typename T, typename ArrayConstructor, typename Region>
jobjectArray jarray_usearch_get(const char *type_name, ArrayConstructor new_array, Region set_array_region, JNIEnv *env,
                                const jlong ptr,
                                const jlong key, const jlong count,
                                const usearch_scalar_kind_t vector_kind) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto dim = usearch_dimensions(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
        return nullptr;
    }

    const auto buf = new T[count * dim];
    const auto actual_count = usearch_get(p, key, count, buf, vector_kind, &err);
    if (err) {
        throw_usearch_exception(env, err);
        return nullptr;
    }

    const auto array = env->NewObjectArray(static_cast<jsize>(actual_count), env->FindClass(type_name), nullptr);
    for (auto i = 0; i < actual_count; ++i) {
        const auto vec = new_array(env, static_cast<jsize>(dim));
        set_array_region(env, vec, 0, static_cast<jsize>(dim), buf + i * dim);
        env->SetObjectArrayElement(array, i, vec);
    }

    delete[] buf;
    return array;
}

extern "C" {
JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1new_1index_1opts(
    JNIEnv *, jobject, jlong dimensions, jint metric_k, jint quantization_k, jlong connectivity, jlong expansion_add,
    jlong expansion_search, jboolean multi) {
    // ReSharper disable once CppDFAMemoryLeak
    auto r = new usearch_init_options_t{
        .metric_kind = static_cast<usearch_metric_kind_t>(metric_k),
        .quantization = static_cast<usearch_scalar_kind_t>(quantization_k),
        .dimensions = static_cast<size_t>(dimensions),
        .connectivity = static_cast<size_t>(connectivity),
        .expansion_add = static_cast<size_t>(expansion_add),
        .expansion_search = static_cast<size_t>(expansion_search),
        .multi = multi == 1
    };
    return reinterpret_cast<jlong>(r);
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1init
(JNIEnv *env, jobject, jlong opts) {
    usearch_error_t error = nullptr;
    auto init = usearch_init(reinterpret_cast<usearch_init_options_t *>(opts), &error);
    if (error) {
        throw_usearch_exception(env, error);
        return 0;
    }
    return reinterpret_cast<jlong>(init);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_release_1index_1opts
(JNIEnv *, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_init_options_t *>(ptr);
    delete p;
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1free
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    usearch_free(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1expansion_1add
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto r = usearch_expansion_add(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
        return 0;
    }
    return static_cast<jlong>(r);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1change_1expansion_1add
(JNIEnv *env, jobject, jlong ptr, jlong new_value) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    usearch_change_expansion_add(p, new_value, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1expansion_1search
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto r = usearch_expansion_search(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
    return static_cast<jlong>(r);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1change_1expansion_1search
(JNIEnv *env, jobject, jlong ptr, jlong new_value) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    usearch_change_expansion_search(p, new_value, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1change_1metric_1kind
(JNIEnv *env, jobject, jlong ptr, jlong new_value) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    const auto metric_k = static_cast<usearch_metric_kind_t>(new_value);
    usearch_error_t err = nullptr;
    usearch_change_metric_kind(p, metric_k, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT jstring JNICALL Java_usearch_NativeBridge_usearch_1hardware_1acceleration
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto r = usearch_hardware_acceleration(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
        return nullptr;
    }
    return env->NewStringUTF(r);
}


JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1memory_1usage
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto r = usearch_memory_usage(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
        return -1;
    }
    return r;
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1dimensions
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto r = usearch_dimensions(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
        return -1;
    }
    return static_cast<jlong>(r);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1remove
(JNIEnv *env, jobject, jlong ptr, jlong key) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    usearch_remove(p, key, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1add_1f32
(JNIEnv *env, jobject, jlong ptr, jlong key, jfloatArray vec) {
    const auto p = reinterpret_cast<unum::usearch::index_dense_t *>(ptr);
    const auto arr = env->GetFloatArrayElements(vec, nullptr);
    auto result = p->add(key, arr);
    env->ReleaseFloatArrayElements(vec, arr, JNI_OK);
    if (!result) {
        throw_usearch_exception(env, result.error.release());
    }
}

JNIEXPORT jobjectArray JNICALL Java_usearch_NativeBridge_usearch_1get_1f32
(JNIEnv *env, jobject, jlong ptr, jlong key, jlong count) {
    return jarray_usearch_get<jfloat>(
        "[F", [](JNIEnv *env, jsize dim) { return env->NewFloatArray(dim); },
        [](JNIEnv *env, jfloatArray arr, jsize start, jsize length, jfloat *source) {
            env->SetFloatArrayRegion(arr, start, length, source);
        },
        env, ptr, key, count, usearch_scalar_f32_k);
}

void JNICALL Java_usearch_NativeBridge_usearch_1add_1f64
(JNIEnv *env, jobject, jlong ptr, jlong key, jdoubleArray vec) {
    const auto p = reinterpret_cast<unum::usearch::index_dense_t *>(ptr);
    const auto arr = env->GetDoubleArrayElements(vec, nullptr);
    auto result = p->add(key, arr);
    env->ReleaseDoubleArrayElements(vec, arr, JNI_OK);
    if (!result) {
        throw_usearch_exception(env, result.error.release());
    }
}

JNIEXPORT jobjectArray JNICALL Java_usearch_NativeBridge_usearch_1get_1f64
(JNIEnv *env, jobject, jlong ptr, jlong key, jlong count) {
    return jarray_usearch_get<jdouble>(
        "[D", [](JNIEnv *env, jsize dim) { return env->NewDoubleArray(dim); },
        [](JNIEnv *env, jdoubleArray arr, jsize start, jsize length, jdouble *source) {
            env->SetDoubleArrayRegion(arr, start, length, source);
        },
        env, ptr, key, count, usearch_scalar_f64_k
    );
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1add_1f16
(JNIEnv *env, jobject, jlong ptr, jlong key, jshortArray vec) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    const auto arr = env->GetShortArrayElements(vec, nullptr);
    usearch_error_t err = nullptr;
    usearch_add(p, key, arr, usearch_scalar_f16_k, &err);
    env->ReleaseShortArrayElements(vec, arr, JNI_OK);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT jobjectArray JNICALL Java_usearch_NativeBridge_usearch_1get_1f16
(JNIEnv *env, jobject, jlong ptr, jlong key, jlong count) {
    return jarray_usearch_get<jshort>(
        "[S", [](JNIEnv *env, jsize dim) { return env->NewShortArray(dim); },
        [](JNIEnv *env, jshortArray arr, jsize start, jsize length, jshort *source) {
            env->SetShortArrayRegion(arr, start, length, source);
        },
        env, ptr, key, count, usearch_scalar_f16_k
    );
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1add_1i8
(JNIEnv *env, jobject, jlong ptr, jlong key, jbyteArray vec) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    const auto arr = env->GetByteArrayElements(vec, nullptr);
    usearch_error_t err = nullptr;
    usearch_add(p, key, arr, usearch_scalar_i8_k, &err);
    env->ReleaseByteArrayElements(vec, arr, JNI_OK);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT jobjectArray JNICALL Java_usearch_NativeBridge_usearch_1get_1i8
(JNIEnv *env, jobject, jlong ptr, jlong key, jlong count) {
    return jarray_usearch_get<jbyte>(
        "[B", [](JNIEnv *env, jsize dim) { return env->NewByteArray(dim); },
        [](JNIEnv *env, jbyteArray arr, jsize start, jsize length, jbyte *source) {
            env->SetByteArrayRegion(arr, start, length, source);
        },
        env, ptr, key, count, usearch_scalar_f16_k
    );
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1add_1b1
(JNIEnv *env, jobject, jlong ptr, jlong key, jbyteArray vec) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    const auto arr = env->GetByteArrayElements(vec, nullptr);
    usearch_error_t err = nullptr;
    usearch_add(p, key, arr, usearch_scalar_b1_k, &err);
    env->ReleaseByteArrayElements(vec, arr, JNI_OK);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT jobjectArray JNICALL Java_usearch_NativeBridge_usearch_1get_1b1
(JNIEnv *env, jobject, jlong ptr, jlong key, jlong count) {
    return jarray_usearch_get<jbyte>(
        "[B", [](JNIEnv *env, jsize dim) { return env->NewByteArray(dim); },
        [](JNIEnv *env, jbyteArray arr, jsize start, jsize length, jbyte *source) {
            env->SetByteArrayRegion(arr, start, length, source);
        },
        env, ptr, key, count, usearch_scalar_b1_k
    );
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1search
(JNIEnv *env, jobject, jlong ptr, jfloatArray query, jint count, jlongArray keys, jfloatArray distances) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    const auto arr = env->GetFloatArrayElements(query, nullptr);
    const auto key_arr = env->GetLongArrayElements(keys, nullptr);
    const auto distances_arr = env->GetFloatArrayElements(distances, nullptr);
    usearch_error_t err = nullptr;
    const auto size = usearch_search(p, arr, usearch_scalar_f32_k, static_cast<size_t>(count),
                                     reinterpret_cast<usearch_key_t *>(key_arr), distances_arr, &err);
    if (err) {
        throw_usearch_exception(env, err);
        return 0;
    }
    env->ReleaseFloatArrayElements(query, arr, JNI_OK);
    env->ReleaseLongArrayElements(keys, key_arr, JNI_OK);
    env->ReleaseFloatArrayElements(distances, distances_arr, JNI_OK);
    return static_cast<jlong>(size);
}


JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1size
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto size = usearch_size(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
    return static_cast<jlong>(size);
}


JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1capacity
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto cap = usearch_capacity(p, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
    return static_cast<jlong>(cap);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1reserve
(JNIEnv *env, jobject, jlong ptr, jlong capacity) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    usearch_reserve(p, capacity, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1save_1file
(JNIEnv *env, jobject, jlong ptr, jstring path) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    auto *path_buf = env->GetStringUTFChars(path, nullptr);
    usearch_save(p, path_buf, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
    env->ReleaseStringUTFChars(path, path_buf);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1save_1buffer
(JNIEnv *env, jobject, jlong ptr, jbyteArray buffer) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto buffer_len = env->GetArrayLength(buffer);
    const auto buffer_ptr = env->GetByteArrayElements(buffer, nullptr);

    usearch_save_buffer(
        p,
        buffer_ptr,
        buffer_len,
        &err
    );
    if (err) {
        throw_usearch_exception(env, err);
    }
    env->ReleaseByteArrayElements(buffer, buffer_ptr, JNI_COMMIT);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1load_1file
(JNIEnv *env, jobject, jlong ptr, jstring path) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    auto *path_buf = env->GetStringUTFChars(path, nullptr);
    usearch_load(p, path_buf, &err);
    if (err) {
        throw_usearch_exception(env, err);
    }
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1load_1buffer
(JNIEnv *env, jobject, jlong ptr, jbyteArray buffer) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto buffer_len = env->GetArrayLength(buffer);
    const auto buffer_ptr = env->GetByteArrayElements(buffer, nullptr);
    usearch_load_buffer(
        p,
        buffer_ptr,
        buffer_len,
        &err
    );
    if (err) {
        throw_usearch_exception(env, err);
    }
    env->ReleaseByteArrayElements(buffer, buffer_ptr, JNI_COMMIT);
}
}
