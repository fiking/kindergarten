#include "collector.h"
#include <stdio.h>
#include <stdint.h>
#include "sizes.h"
#include <stdlib.h>
#include <iostream>

#define DEBUGRC 1

// #define OFFSET_RC OFFSET_GC_HEADER
// #define OFFSET_N_REFS GC_EXTRA_SIZE
// #define OFFSET_OFFSETS GC_EXTRA_SIZE + sizeof(size_t)

namespace maplert {
void mapleRT_incRef(object_t *obj) {
    if (obj == 0) return;
    uint32_t *rc = (uint32_t*)((uint8_t*)obj + OFFSET_RC);
    *rc += 1;

    if (DEBUGRC)
        std::cout << "Incref: " << std::hex << (uintptr_t)obj
                  << std::dec << " rc: " << *rc  << std::endl;
}

void mapleRT_decRef(object_t *obj) {
    if (obj == 0) return;
    uint32_t *rc = (uint32_t*)((uint8_t*)obj + OFFSET_RC);
    *rc -= 1;
    if (DEBUGRC)
        std::cout << "Decref: " << std::hex << (uintptr_t)obj
                  << std::dec << " rc: " << *rc  << std::endl;

    if (*rc == 0) {
        decChildren(obj);
        std::cout << "Freeing object: " << std::hex << (uintptr_t)obj << std::dec << std::endl;
        free(obj); // Assuming mapleRT_freeobj is defined elsewhere
    }
}

void decChildren(object_t *obj) {
    uintptr_t *gctib = (uintptr_t*)((uint8_t*)obj + OFFSET_GCTIB_PTR);
    size_t numRefs = *(size_t*)((uint8_t*)gctib + OFFSET_N_REFS);
    offset_t *refs = (offset_t*)((uint8_t*)gctib + OFFSET_OFFSETS);
    for (size_t i = 0; i < numRefs; ++i) {
        uintptr_t refAddr = *(uintptr_t*)((uint8_t*)obj + refs[i]);
        mapleRT_decRef(reinterpret_cast<object_t*>(refAddr));
    }
}

} // namespace maplert
