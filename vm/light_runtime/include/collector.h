#ifndef COLLECTOR_H
#define COLLECTOR_H
#include "memorymanager.h"

namespace maplert {
typedef void object_t;
extern "C" {
void mapleRT_incRef(address_t obj);
void mapleRT_decRef(address_t obj);
}

void triggerGC();
} // namespace maplert
#endif // COLLECTOR_H
