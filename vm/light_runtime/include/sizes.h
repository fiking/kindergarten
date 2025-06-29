#ifndef SIZES_H
#define SIZES_H
#include <cstdint>
#include "memorymanager.h"

#include <cstddef>
#include <sys/types.h>

#ifdef __cplusplus
namespace maplert {
#endif

// 在此处定义与对象/类型大小相关的常量或宏
// 例如：
// #define MAPLERT_OBJECT_HEADER_SIZE 16
const size_t HWORD_BYTES = 2;
const size_t WORD_BYTES = 4;
const size_t DWORD_BYTES = 8; // 假设双字大小为8字节
const size_t QWORD_BYTES = 16; // 假设四字大小为16字节

const size_t HEADER_SIZE = DWORD_BYTES * 2; // 假设对象头部大小为两个字

const offset_t OFFSET_GCTIB_PTR = -DWORD_BYTES; // 偏移量指向GC TIB的指针
const offset_t OFFSET_LOCK = -(DWORD_BYTES + WORD_BYTES);
const offset_t OFFSET_GC_HEADER = -(DWORD_BYTES * 2); // 偏移量指向引用计数

struct GCTIB_GCInfo {
    offset_t array_content_offset; // 数组内容的偏移量
    offset_t array_length_offset; // 数组长度的偏移量
    size_t n_bitmap_words; // 位图字数
    uint64_t bitmap_words[]; // 位图数组
};

static const offset_t JAVA_ARRAY_CONTENT_OFFSET = 32; // Java数组内容的偏移量
static const offset_t JAVA_ARRAY_LENGTH_OFFSET = 28; // Java数组长度的偏移量

inline address_t &gctibPtr(address_t objaddr) {
    return *reinterpret_cast<address_t*>(objaddr + OFFSET_GCTIB_PTR);
}

inline GCTIB_GCInfo &gcInfo(address_t objaddr) {
    address_t gctib = gctibPtr(objaddr);
    uint64_t user_data_size = *reinterpret_cast<uint64_t*>(gctib);
    return *reinterpret_cast<GCTIB_GCInfo*>(gctib + user_data_size);
}

inline uint32_t &gcHeader(address_t objaddr) {
    return *reinterpret_cast<uint32_t*>(objaddr + OFFSET_GC_HEADER);
}

template<class UnaryFunction>
inline void forEachRefField(address_t objaddr, UnaryFunction func) {
    GCTIB_GCInfo &gctibInfo = gcInfo(objaddr);
    size_t n_bitmap_words = gctibInfo.n_bitmap_words;
    uint64_t *bitmap_words = gctibInfo.bitmap_words;
    intptr_t current_word_offset = 0;
    for (size_t i = 0; i < n_bitmap_words; ++i) {
        uint64_t bitmap_word = bitmap_words[i];
        intptr_t current_in_word_offset = 0;
        while (bitmap_word > 0) {
            if (bitmap_word & 1) {
                // This bit is set, indicating a reference to an object
                uintptr_t current_offset = current_word_offset + current_in_word_offset;
                address_t child_obj_addr = objaddr + current_offset;

                // Decrement the reference count of the child object
                func(*reinterpret_cast<address_t*>(child_obj_addr));
            }
            bitmap_word >>= 1; // Shift right to check the next bit
            current_in_word_offset += DWORD_BYTES;
        }        
        current_word_offset += DWORD_BYTES * 64; // Move to the next bitmap word
    }
}
#ifdef __cplusplus
} // namespace maplert
#endif

#endif // SIZES_H
