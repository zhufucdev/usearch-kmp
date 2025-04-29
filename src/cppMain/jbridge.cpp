#include <iostream>
#include <usearch/index_dense.hpp>

#include "jexceptions.h"

extern "C" {
#include "lib.h"
#include "usearch_NativeBridge.h"
}

using search_result_t = unum::usearch::index_dense_gt<>::search_result_t;

template<typename T>
struct value_proxy {
    T inner;
};

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
        throw_runtime_error(env, error);
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
        throw_runtime_error(env, err);
    }
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1expansion_1add
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto r = usearch_expansion_add(p, &err);
    if (err) {
        throw_runtime_error(env, err);
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
        throw_runtime_error(env, err);
    }
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1expansion_1search
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto r = usearch_expansion_search(p, &err);
    if (err) {
        throw_runtime_error(env, err);
    }
    return static_cast<jlong>(r);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1change_1expansion_1search
(JNIEnv *env, jobject, jlong ptr, jlong new_value) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    usearch_change_expansion_search(p, new_value, &err);
    if (err) {
        throw_runtime_error(env, err);
    }
}


JNIEXPORT jstring JNICALL Java_usearch_NativeBridge_usearch_1hardware_1acceleration
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    const auto r = usearch_hardware_acceleration(p, &err);
    if (err) {
        throw_runtime_error(env, err);
        return nullptr;
    }
    return env->NewStringUTF(r);
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1add_1f32
(JNIEnv *env, jobject, jlong ptr, jlong key, jfloatArray vec) {
    const auto p = reinterpret_cast<unum::usearch::index_dense_t *>(ptr);
    const auto vec_len = env->GetArrayLength(vec);
    auto *buf = new jfloat[vec_len];
    env->GetFloatArrayRegion(vec, 0, vec_len, buf);

    auto result = p->add(key, buf);
    delete[] buf;
    if (!result) {
        throw_runtime_error(env, result.error.release());
    }
}

JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1add_1f64
(JNIEnv *env, jobject, jlong ptr, jlong key, jdoubleArray vec) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    const auto vec_len = env->GetArrayLength(vec);
    auto *buf = new jdouble[vec_len];
    env->GetDoubleArrayRegion(vec, 0, vec_len, buf);

    usearch_error_t err = nullptr;
    usearch_add(p, key, buf, usearch_scalar_f64_k, &err);
    delete[] buf;
    if (err) {
        throw_runtime_error(env, err);
    }
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1search
(JNIEnv *env, jobject, jlong ptr, jfloatArray query, jint count) {
    const auto p = reinterpret_cast<unum::usearch::index_dense_t *>(ptr);
    const auto query_len = env->GetArrayLength(query);
    auto *query_buf = new jfloat[query_len];
    auto result = p->search(query_buf, count);
    delete[] query_buf;
    if (!result) {
        throw_runtime_error(env, result.error.release());
        return 0;
    }
    return reinterpret_cast<jlong>(new value_proxy<search_result_t>{std::move(result)});
}

JNIEXPORT jlong JNICALL Java_usearch_NativeBridge_usearch_1sresult_1key_1at
(JNIEnv *env, jobject, jlong ptr, jint index) {
    const auto p = reinterpret_cast<value_proxy<search_result_t> *>(ptr);
    if (p->inner.count < index) {
        throw_index_out_of_bounds(env, index);
        return 0;
    }
    return p->inner[index].member.key;
}


JNIEXPORT jfloat JNICALL Java_usearch_NativeBridge_usearch_1sresult_1distance_1at
(JNIEnv *env, jobject, jlong ptr, jint index) {
    const auto p = reinterpret_cast<value_proxy<search_result_t> *>(ptr);
    if (p->inner.count < index) {
        throw_index_out_of_bounds(env, index);
        return 0;
    }
    return p->inner[index].distance;
}

JNIEXPORT jint JNICALL Java_usearch_NativeBridge_usearch_1sresult_1size
(JNIEnv *env, jobject, jlong ptr) {
    const auto p = reinterpret_cast<value_proxy<search_result_t> *>(ptr);
    return static_cast<jint>(p->inner.count);
}


JNIEXPORT void JNICALL Java_usearch_NativeBridge_usearch_1reserve
(JNIEnv *env, jobject, jlong ptr, jlong capacity) {
    const auto p = reinterpret_cast<usearch_index_t *>(ptr);
    usearch_error_t err = nullptr;
    usearch_reserve(p, capacity, &err);
    if (err) {
        throw_runtime_error(env, err);
    }
}
}
