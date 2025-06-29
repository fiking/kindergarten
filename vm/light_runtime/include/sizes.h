#ifndef SIZES_H
#define SIZES_H
#include <cstdint>
#include "memorymanager.h"

#define MAPLERT_GC_STRATEGY_RC 1
#define MAPLERT_GC_STRATEGY_MS 2

#if !defined(MAPLERT_GC_STRATEGY)
#define MAPLERT_GC_STRATEGY MAPLERT_GC_STRATEGY_MS
#endif // !defined(MAPLERT_GC_STRATEGY)

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
#ifdef __cplusplus
} // namespace maplert
#endif

#endif // SIZES_H
