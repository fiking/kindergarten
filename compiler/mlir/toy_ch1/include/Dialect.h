#ifndef DIALECT_H
#define DIALECT_H

#include "mlir/IR/Dialect.h"
#include "mlir/IR/FunctionInterfaces.h"
#include "mlir/IR/SymbolTable.h"
#include "mlir/Interfaces/CallInterfaces.h"
#include "mlir/Interfaces/SideEffectInterfaces.h"
#include "mlir/Interfaces/CastInterfaces.h"

#include "ShapeInferenceInterface.h"

/// Include the auto-generated header file containing the declaration of the toy
/// dialect.
#include "Dialect.h.inc"

/// Include the auto-generated header file containing the declarations of the
/// toy operations.
#define GET_OP_CLASSES
#include "Ops.h.inc"

#endif // DIALECT_H