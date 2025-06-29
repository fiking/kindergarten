#include "memorymanager.h"
#include <cstdlib>
#include "stackunwinder.h"

namespace maplert {
// global GC states
std::vector<address_t> g_all_objects; // 全局对象列表，用于跟踪所有分配的对象
std::vector<address_t> g_objects_allocated; 
std::vector<address_t> g_objects_freed; // 全局已释放对象列表，用于跟踪已释放的对象

static FrameCursorFactory _the_frame_cursor_factory;
FrameCursorFactory *g_frame_cursor_factory = &_the_frame_cursor_factory;

// thread-local GC states
thread_local address_t tl_gc_stack_low_water_mark;

bool mapleRT_init_allocator_global() {
    return true;
}

bool mapleRT_fini_allocator_global() {
    return true;
}

bool mapleRT_init_allocator_threadlocal()
{
    void *caller_fp = __builtin_frame_address(1);
    tl_gc_stack_low_water_mark = reinterpret_cast<address_t>(caller_fp);
    return false;
}

bool mapleRT_fini_allocator_threadlocal()
{
    return false;
}

void mapleRT_yeildpoint()
{
}

} // namespace maplert
