#ifndef COLLECTOR_H
#define COLLECTOR_H

#ifdef __cplusplus
namespace maplert {
extern "C" {
#endif
typedef void object_t;

void mapleRT_incRef(object_t *obj);
void mapleRT_decRef(object_t *obj);

void decChildren(object_t *obj);

#ifdef __cplusplus
} // extern "C"
} // namespace maplert
#endif

#endif // COLLECTOR_H
