LLVM_HOME=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project
PATH=$LLVM_HOME/build/bin:$PATH

mlir-tblgen -gen-dialect-decls toy_dialect.td -I $LLVM_HOME/mlir/include
mlir-tblgen -gen-op-defs ops.td -I $LLVM_HOME/mlir/include

