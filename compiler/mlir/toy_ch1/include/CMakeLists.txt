# set(MLIR_TABLEGEN_EXE mlir-tblgen)
set(LLVM_TARGET_DEFINITIONS Ops.td)
mlir_tablegen(Ops.h.inc -gen-op-decls)
add_public_tablegen_target(ToyOpsIncGen)