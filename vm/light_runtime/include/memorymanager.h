#ifndef MEMORYMANAGER_H
#define MEMORYMANAGER_H

#include <cstddef>
#include <cstdint>
#include <cstdlib>

namespace maplert {
// 对象类型定义
typedef void object_t;
typedef object_t *objref_t;
typedef uintptr_t address_t;
typedef intptr_t offset_t;

// thread-local GC states
extern thread_local address_t tl_gc_stack_low_water_mark;

extern "C" {
// 分配器初始化，返回true表示成功，false表示失败
bool mapleRT_init_allocator_global();

// 分配器回收，返回true表示成功，false表示失败
bool mapleRT_fini_allocator_global();

// 分配器初始化，返回true表示成功，false表示失败
bool mapleRT_init_allocator_threadlocal();

// 分配器回收，返回true表示成功，false表示失败
bool mapleRT_fini_allocator_threadlocal();
}
} // namespace maplert

#endif // MEMORYMANAGER_H
