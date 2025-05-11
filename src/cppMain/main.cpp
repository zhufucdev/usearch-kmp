#include "lib.h"
#include <iostream>

float *produce_query(float offset) {
    auto vec = new float[512];
    for (auto j = 0; j < 512; ++j) {
        vec[j] = j + offset;
    }
    return vec;
}

int main(int argc, char *argv[]) {
    auto opts = usearch_init_options_t {
        .metric_kind = usearch_metric_cos_k,
        .quantization = usearch_scalar_f16_k,
        .dimensions = 512
    };
    usearch_error_t err = nullptr;
    const auto index = usearch_init(&opts, &err);
    if (err) {
        std::cerr << err << std::endl;
        abort();
    }
    usearch_reserve(index, 72, &err);
    if (err) {
        std::cerr << err << std::endl;
        abort();
    }
    for (auto i = 0; i < 72; ++i) {
        const auto query = produce_query(i);
        usearch_add(index, i, query, usearch_scalar_f32_k, &err);
        delete[] query;
        if (err) {
            std::cerr << err << std::endl;
            abort();
        }
    }
    const auto buf_len = usearch_serialized_length(index, &err);
    auto buf = new char[buf_len];
    if (err) {
        std::cerr << err << std::endl;
        abort();
    }
    usearch_save_buffer(index, buf, buf_len, &err);
    if (err) {
        std::cerr << err << std::endl;
        abort();
    }

    const auto index_loaded = usearch_init(&opts, &err);
    if (err) {
        std::cerr << err << std::endl;
        abort();
    }

    usearch_load_buffer(index_loaded, buf, buf_len, &err);
    if (err) {
        std::cerr << err << std::endl;
        abort();
    }
    delete[] buf;

    for (auto key = 0; key < 72; ++key) {
        usearch_key_t keys[10];
        float distances[10];
        auto query = produce_query(key);
        usearch_search(index, query, usearch_scalar_f32_k, 10, keys, distances, &err);
        delete[] query;
        if (err) {
            std::cerr << err << std::endl;
            abort();
        }
    }

    usearch_free(index, nullptr);
    usearch_free(index_loaded, nullptr);
}
