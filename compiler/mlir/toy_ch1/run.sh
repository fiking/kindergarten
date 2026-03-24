#!/bin/bash

# 快速运行编译器的脚本

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 检查可执行文件是否存在
if [ ! -f "$SCRIPT_DIR/build/compiler" ]; then
    echo "❌ 编译器未找到，正在编译..."
    echo ""

    # 编译
    mkdir -p "$SCRIPT_DIR/build"
    cd "$SCRIPT_DIR/build" || exit 1

    LLVM_DIR="/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm"

    if ! cmake -DLLVM_DIR="$LLVM_DIR" ..; then
        echo "❌ CMake 配置失败"
        exit 1
    fi

    if ! cmake --build .; then
        echo "❌ 编译失败"
        exit 1
    fi

    cd "$SCRIPT_DIR" || exit 1
fi

# 运行编译器
echo "🚀 运行编译器..."
echo ""
$SCRIPT_DIR/build/compiler ~/code/ArtificialIntelligence/compiler/llvm-project/mlir/test/Examples/Toy/Ch2/codegen.toy -emit=mlir
