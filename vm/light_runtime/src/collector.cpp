#include "collector.h"
#include <cstdint>
#include "sizes.h"
#include <cstdlib>
#include <iostream>

#define DEBUGRC 0

#define OFFSET_RC OFFSET_GC_HEADER

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
    GCTIB_GCInfo *gctibInfo = (GCTIB_GCInfo*)gctib;
    size_t n_bitmap_words = gctibInfo->n_bitmap_words;
    uint64_t *bitmap_words = gctibInfo->bitmap_words;
    intptr_t current_word_offset = 0;

    for (size_t i = 0; i < n_bitmap_words; ++i) {
        uint64_t bitmap_word = bitmap_words[i];
        intptr_t current_in_word_offset = 0;
        while (bitmap_word > 0) {
            if (bitmap_word & 1) {
                // This bit is set, indicating a reference to an object
                uintptr_t child_obj_addr = (uintptr_t)obj + current_word_offset + current_in_word_offset;
                object_t *child_obj = (object_t*)child_obj_addr;

                // Decrement the reference count of the child object
                mapleRT_decRef(child_obj);
            }
            bitmap_word >>= 1; // Shift right to check the next bit
            current_in_word_offset += DWORD_BYTES;
        }        
        current_word_offset += DWORD_BYTES * 64; // Move to the next bitmap word
    }
}

void triggerGC()
{
}
}
