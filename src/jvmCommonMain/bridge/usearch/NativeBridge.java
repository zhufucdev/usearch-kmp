package usearch;

public class NativeBridge {
    public native long usearch_new_index_opts(long dimensions,
                                                     int metric_k,
                                                     int quantization_k,
                                                     long connectivity,
                                                     long expansion_add,
                                                     long expansion_search,
                                                     boolean multi) throws RuntimeException;

    public native long usearch_init(long options_ptr) throws RuntimeException;

    public native void release_index_opts(long ptr) throws RuntimeException;

    public native void usearch_free(long index_ptr) throws RuntimeException;

    public native long usearch_expansion_add(long index_ptr);

    public native void usearch_change_expansion_add(long index_ptr, long new_value);

    public native long usearch_expansion_search(long index_ptr);

    public native void usearch_change_expansion_search(long index_ptr, long new_value);

    public native String usearch_hardware_acceleration(long index_ptr);

    public native void usearch_add_f32(long index_ptr, long key, float[] f32_vec);

    public native void usearch_add_f64(long index_ptr, long key, double[] f64_vec);

    public native long usearch_search(long index_ptr, float[] query, int count);

    public native long usearch_sresult_key_at(long ptr, int index);

    public native float usearch_sresult_distance_at(long ptr, int index);

    public native int usearch_sresult_size(long ptr);

    public native void usearch_reserve(long ptr, long capacity);

    public native long usearch_size(long ptr);

    public native long usearch_capacity(long ptr);

    public native void usearch_save_file(long ptr, String file_path);

    public native void usearch_save_buffer(long ptr, byte[] buffer);

    public native void usearch_load_file(long ptr, String file_path);

    public native void usearch_load_buffer(long ptr, byte[] buffer);
}
