#ifndef ALLOCATOR_H
#define ALLOCATOR_H

#include <stddef.h>
#include <stdbool.h>

#ifdef __cplusplus
namespace maplert {
extern "C" {
#endif

// 对象类型定义
typedef void object_t;

// 创建对象，size为分配字节数，zero为是否清零
object_t *mapleRT_newobj(size_t size, size_t align, bool zero = true);

// 释放对象
void mapleRT_freeobj(object_t *obj);

// 分配器初始化，返回true表示成功，false表示失败
bool mapleRT_init_allocator_global();

// 分配器回收，返回true表示成功，false表示失败
bool mapleRT_fini_allocator_global();

// 分配器初始化，返回true表示成功，false表示失败
bool mapleRT_init_allocator_threadlocal();

// 分配器回收，返回true表示成功，false表示失败
bool mapleRT_fini_allocator_threadlocal();

#ifdef __cplusplus
} // extern "C"
} // namespace maplert
#endif

#endif // ALLOCATOR_H
