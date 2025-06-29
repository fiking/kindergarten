#ifndef COLLECTOR_H
#define COLLECTOR_H

namespace maplert {
typedef void object_t;
extern "C" {
void mapleRT_incRef(object_t *obj);
void mapleRT_decRef(object_t *obj);
} // extern "C"

void decChildren(object_t *obj);

void triggerGC();
} // namespace maplert
#endif // COLLECTOR_H
