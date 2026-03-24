// Defines the oprerations of our toy language.
// This file is used by TableGen to generate the corresponding C++ code for the operations.

include "mlir/IR/OpBase.td"

#ifndef TOY_OPS_H
#define TOY_OPS_H

def Toy_Dialect : Dialect {
  let name = "toy";
  let cppNamespace = "::mlir::toy";
  let emitAccessorPrefix = kEmitAccessorPrefix_Prefixed;
}

#endif // TOY_OPS_H