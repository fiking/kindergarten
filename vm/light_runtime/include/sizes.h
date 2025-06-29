#ifndef SIZES_H
#define SIZES_H

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
const size_t WORD_SIZE_BYTES = sizeof(size_t); // 假设指针大小为字长
#if MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_RC
const size_t HEADER_SIZE = 2 * WORD_SIZE_BYTES; // 对象头部大小为2个字长
#elif MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_MS
const size_t HEADER_SIZE = WORD_SIZE_BYTES; // 对象头部大小为1个字长
#else
#error "Unsupported GC strategy"
#endif

typedef ssize_t offset_t; // 偏移量类型，通常为size_t
const offset_t OFFSET_GCTIB_PTR = -WORD_SIZE_BYTES; // 偏移量指向GC TIB的指针
const offset_t OFFSET_RC = -WORD_SIZE_BYTES * 2; // 偏移量指向引用计数

struct GCTIB {
    size_t n_refs; // 引用计数
    offset_t offsets[]; // 偏移量数组
};

const offset_t OFFSET_N_REFS = offsetof(GCTIB, n_refs); // 偏移量指向引用计数
const offset_t OFFSET_OFFSETS = offsetof(GCTIB, offsets); // 偏移量指向偏移量数组
#ifdef __cplusplus
} // namespace maplert
#endif

#endif // SIZES_H
