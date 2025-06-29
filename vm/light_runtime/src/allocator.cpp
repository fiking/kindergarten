#include "allocator.h"
#include <cstdlib>
#include <cstring>

#include "sizes.h"
#include "memorymanager.h"

namespace maplert {
const size_t HEADER_ALLOC_SIZE = HEADER_SIZE; // Placeholder for header allocation size

object_t *mapleRT_newobj(size_t size, size_t align, bool zero) {
    size_t alloc_size = size + HEADER_ALLOC_SIZE; // Include header size
    object_t *alloca_ptr = calloc(1, alloc_size);
    uintptr_t alloc_addr = reinterpret_cast<uintptr_t>(alloca_ptr);
    uintptr_t result_addr = alloc_addr + HEADER_ALLOC_SIZE;
    object_t *result_ptr = reinterpret_cast<object_t *>(result_addr);

    g_objects_allocated.push_back(result_addr); // Track allocated object
    return result_ptr;
}

void mapleRT_freeobj(object_t *obj) {
    uintptr_t obj_addr = reinterpret_cast<uintptr_t>(obj);
    uintptr_t alloc_addr = obj_addr - HEADER_ALLOC_SIZE; // Adjust back to the original
    object_t *alloca_ptr = reinterpret_cast<object_t *>(alloc_addr);

    g_objects_freed.push_back(obj_addr); // Track freed object
    // Free the memory including the header
    free(alloca_ptr);
}
} // namespace maplert
