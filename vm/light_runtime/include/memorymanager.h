#ifndef MEMORYMANAGER_H
#define MEMORYMANAGER_H

#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <vector>

#define MAPLERT_GC_STRATEGY_RC 1
#define MAPLERT_GC_STRATEGY_MS 2

#if !defined(MAPLERT_GC_STRATEGY)
#define MAPLERT_GC_STRATEGY MAPLERT_GC_STRATEGY_MS
#endif // !defined(MAPLERT_GC_STRATEGY)

namespace maplert {
// 对象类型定义
typedef void object_t;
typedef object_t *objref_t;
typedef uintptr_t address_t;
typedef intptr_t offset_t;

// global GC states
extern std::vector<address_t> g_all_objects; // 全局对象列表，用于跟踪所有分配的对象
extern std::vector<address_t> g_objects_allocated; 
extern std::vector<address_t> g_objects_freed; // 全局已释放对象列表，用于跟踪已释放的对象

class FrameCursorFactory;
extern FrameCursorFactory *g_frame_cursor_factory;

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

void mapleRT_yeildpoint();
}
} // namespace maplert

#endif // MEMORYMANAGER_H
