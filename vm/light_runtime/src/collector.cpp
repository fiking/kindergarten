#include "collector.h"
#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <cassert>

#include "sizes.h"
#include "stackunwinder.h"
#include "allocator.h"

#define DEBUGRC 0

#define OFFSET_RC OFFSET_GC_HEADER

namespace maplert {
void mapleRT_incRef(address_t obj) {
    if (obj == 0) return;
    uint32_t &rc = gcHeader(obj);
    rc += 1;
    if (DEBUGRC)
        std::cout << "Incref: " << std::hex << (uintptr_t)obj
                  << std::dec << " rc: " << rc  << std::endl;
}

void decChildren(address_t obj) {
    forEachRefField(obj, [](address_t child) { mapleRT_decRef(child); });
}

void mapleRT_decRef(address_t obj) {
    if (obj == 0) return;
    uint32_t &rc = gcHeader(obj);
    rc -= 1;
    if (DEBUGRC)
        std::cout << "Decref: " << std::hex << (uintptr_t)obj
                  << std::dec << " rc: " << rc  << std::endl;

    if (rc == 0) {
        decChildren(obj);
        std::cout << "Freeing object: " << std::hex << (uintptr_t)obj << std::dec << std::endl;
        free(reinterpret_cast<void*>(obj)); // Assuming mapleRT_freeobj is defined elsewhere
    }
}

/// Mark-Sweep implementation
const uint32_t MARK_BIT = 0x1;

void inplace_difference(std::vector<address_t> &a, const std::vector<address_t> &b) {
    size_t cursor_r = 0;
    size_t cursor_w = 0;
    size_t asize = a.size();
    for (auto belem : b) {
        while (cursor_r < asize && a[cursor_r] < belem) {
            a[cursor_w++] = a[cursor_r++];
        }
        if (cursor_r < asize && a[cursor_r] == belem) {
            cursor_r++;
        }
    }
    a.resize(cursor_w);
}

void applyLoggedAllocFree() {
    sort(g_objects_allocated.begin(), g_objects_allocated.end());
    sort(g_objects_freed.begin(), g_objects_freed.end());
    size_t old_size = g_all_objects.size();
    move(g_objects_allocated.begin(), g_objects_allocated.end(), back_inserter(g_all_objects));
    inplace_difference(g_all_objects, g_objects_freed);
    g_objects_allocated.clear();
    g_objects_freed.clear();
}

bool markObject(address_t obj) {
    address_t old_gcheader = gcHeader(obj);
#if MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_RC
    gcHeader(obj)++;
    return old_gcheader == 0;
#else MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_MS
    gcHeader(obj) |= MARK_BIT;
    return !(old_gcheader & MARK_BIT);
#endif
}

void unmarkObject(address_t obj) {
    assert(MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_MS && "Unmarking is only valid for MS GC strategy");
    gcHeader(obj) &= ~MARK_BIT;
}

bool isObjectMarked(address_t obj) {
    address_t gcheader = gcHeader(obj);
#if MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_RC
    return gcheader != 0;
#else // MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_MS
    return (gcheader & MARK_BIT) != 0;
#endif
}

void resetRefCounts() {
    assert(MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_RC && "Resetting reference counts is only valid for RC GC strategy");
    // Reset all reference counts to 0
    for (auto &obj : g_all_objects) {
        gcHeader(obj) = 0; // Reset reference counts to 0
    }
}

void maybeEnqueue(address_t data, std::vector<address_t> &root_set) {
    if ((data & 7) == 0) { // Check if data is aligned to 8 bytes
        if (binary_search(g_all_objects.begin(), g_all_objects.end(), data)) {
            root_set.push_back(data);
        }
    }
}

void scanStackConservative(uintptr_t regs_addr, std::vector<address_t> &root_set) {
    FrameCursor cursor = g_frame_cursor_factory->NewFrameCursor(reinterpret_cast<uintptr_t*>(regs_addr));
    // This function scans the stack conservatively for root references.
    // It assumes that the stack is aligned and uses the registers to find potential roots.
    for (size_t i = AArch64State::GPREGS_CALLEESAVED_FIRST; i < AArch64State::GPREGS_CALLEESAVED_LAST; ++i) {
        maybeEnqueue(cursor.state.gpregs[i], root_set);
    }

    uintptr_t low_water_mark = tl_gc_stack_low_water_mark;
    for (uintptr_t cur_addr = cursor.state.sp; cur_addr < low_water_mark; cur_addr += sizeof(address_t)) {
        address_t cur_ptr = *reinterpret_cast<address_t*>(cur_addr);
        maybeEnqueue(cur_ptr, root_set);
    }
}

void scanStackRoots(uintptr_t regs_addr, std::vector<address_t> &root_set) {
    // This function scans the stack for root references.
    // It uses the registers to find potential roots and adds them to the root_set.
    scanStackConservative(regs_addr, root_set);
}

void scanGlobalRoots(std::vector<address_t> &root_set) {
    // TODO: Implement global root scanning logic.
}

void scanJNIRoots(std::vector<address_t> &root_set) {
}

void enqueueNeighbors(address_t obj, std::vector<address_t> &work_stack) {
    // This function enqueues the neighbors of the given object into the work stack.
    // It scans the reference fields of the object and adds them to the work stack if they are not marked.
    forEachRefField(obj, [&work_stack](address_t child) {
        work_stack.push_back(child);
    });
}

void doTransitiveClosure(std::vector<address_t> &work_stack) {
    while (!work_stack.empty()) {
        address_t obj = work_stack.back();
        work_stack.pop_back();

        bool newly_marked = markObject(obj);
        if (!newly_marked) {
            continue; // If the object was already marked, skip it
        }
        enqueueNeighbors(obj, work_stack);
    }
}

void sweep() {
    size_t live_index = 0;
    for (auto objaddr : g_all_objects) {
        if (!isObjectMarked(objaddr)) {
            mapleRT_freeobj(reinterpret_cast<object_t*>(objaddr));
            g_all_objects[live_index] = objaddr;
            live_index++;
        } else {
#if MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_MS
            unmarkObject(objaddr);
#endif            
        }
    }

    g_all_objects.resize(live_index);
}

void runMarkSweep(uintptr_t regs_addr) {
    applyLoggedAllocFree();
    std::vector<address_t> root_set;
#if MAPLERT_GC_STRATEGY == MAPLERT_GC_STRATEGY_RC
    resetRefCounts();
#endif
    scanJNIRoots(root_set);
    scanGlobalRoots(root_set);
    scanStackConservative(regs_addr, root_set);

    std::vector<address_t> work_stack = std::move(root_set);
    doTransitiveClosure(root_set);
    sweep();
}

uintptr_t handleTriggeredGC(uintptr_t regs_addr, void *unused) {
    runMarkSweep(regs_addr);
}


void triggerGC() {
    mapleRT__save_registers_and_run(handleTriggeredGC, nullptr);
}

uintptr_t mapleRT__yieldpoint_handler(uintptr_t regs_addr, void *userdata) {
    return 0; // Return value can be adjusted based on the GC logic
}
}
