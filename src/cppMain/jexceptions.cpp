//
// Created by Steve Reed on 2025/4/28.
//

#include "jexceptions.h"

void throw_runtime_error(JNIEnv *env, char const *msg) {
    const auto err_cls = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(err_cls, msg);
}

void throw_index_out_of_bounds(JNIEnv *env, const jint index) {
    const auto err_cls = env->FindClass("java/lang/IndexOutOfBoundsException");
    const auto err_constructor_method = env->GetMethodID(err_cls, "<init>", "(I)V");
    const auto err = env->NewObject(err_cls, err_constructor_method, index);
    env->Throw(reinterpret_cast<jthrowable>(err));
}
