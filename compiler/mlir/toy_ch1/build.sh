#!/bin/bash

# Simple build script for LLVM Compiler

set -e

echo "========== Building Simple LLVM Compiler =========="
echo ""

# 本地LLVM项目路径
LOCAL_LLVM_PROJECT="/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project"
LLVM_CMAKE_DIR="${LOCAL_LLVM_PROJECT}/build/lib/cmake/llvm"

# 检查本地LLVM是否存在
if [ ! -d "$LLVM_CMAKE_DIR" ]; then
    echo "Error: Local LLVM not found at $LLVM_CMAKE_DIR"
    echo "Please ensure LLVM is built at $LOCAL_LLVM_PROJECT"
    exit 1
fi

echo "Using local LLVM from: $LOCAL_LLVM_PROJECT"
echo ""

# 检查是否有构建目录
if [ ! -d "build" ]; then
    echo "Creating build directory..."
    mkdir -p build
fi

cd build

# 检查 CMakeCache.txt 是否存在（表示已配置过）
if [ ! -f "CMakeCache.txt" ]; then
    echo "Running CMake configuration..."
    cmake -DLLVM_DIR="$LLVM_CMAKE_DIR" ..
    echo ""
fi

echo "Building project..."
cmake --build . --parallel $(nproc 2>/dev/null || sysctl -n hw.ncpu)

echo ""
echo "========== Build Complete =========="
echo ""
echo "To run the compiler, use:"
echo "    ./build/compiler"
echo ""
