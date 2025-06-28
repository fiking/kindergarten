#include "allocator.h"
#include <stdlib.h>
#include <string.h>

bool mapleRT_init_allocator_global() {
    return true;
}

bool mapleRT_fini_allocator_global() {
    return true;
}

object_t *mapleRT_newobj(size_t size, bool zero) {
    return malloc(size);
}

void mapleRT_freeobj(object_t *obj) {
    free(obj);
}
