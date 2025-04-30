//
// Created by Steve Reed on 2025/4/28.
//

#ifndef JEXCEPTIONS_H
#define JEXCEPTIONS_H

#include <jni.h>

void throw_runtime_error(JNIEnv *env, char const *msg);


void throw_usearch_exception(JNIEnv *env, char const *msg);

void throw_index_out_of_bounds(JNIEnv *env, const jint index);

#endif //JEXCEPTIONS_H
