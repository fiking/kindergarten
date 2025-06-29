#include "memorymanager.h"
#include <cstdlib>

namespace maplert {
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

} // namespace maplert
