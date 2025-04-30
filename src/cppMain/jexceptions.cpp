//
// Created by Steve Reed on 2025/4/28.
//

#include "jexceptions.h"
#include <sstream>

void throw_runtime_error(JNIEnv *env, char const *msg) {
    const auto err_cls = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(err_cls, msg);
}

void throw_usearch_exception(JNIEnv *env, char const *msg) {
    const auto err_cls = env->FindClass("usearch/USearchException");
    env->ThrowNew(err_cls, msg);
}

void throw_index_out_of_bounds(JNIEnv *env, const jint index) {
    const auto err_cls = env->FindClass("java/lang/IndexOutOfBoundsException");
    std::stringstream ss;
    ss << "index " << index << " is out of bounds";
    env->ThrowNew(err_cls, ss.str().c_str());
}
