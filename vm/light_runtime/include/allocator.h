#ifndef ALLOCATOR_H
#define ALLOCATOR_H

#include <stddef.h>
#include <stdbool.h>
#include "memorymanager.h"

#ifdef __cplusplus
namespace maplert {
extern "C" {
#endif

// 创建对象，size为分配字节数，zero为是否清零
object_t *mapleRT_newobj(size_t size, size_t align, bool zero = true);

// 释放对象
void mapleRT_freeobj(object_t *obj);
#ifdef __cplusplus
} // extern "C"
} // namespace maplert
#endif

#endif // ALLOCATOR_H
