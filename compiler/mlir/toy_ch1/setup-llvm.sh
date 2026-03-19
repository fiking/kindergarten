#!/bin/bash

# LLVM环境设置脚本
# 此脚本设置本地LLVM的环境变量

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 源配置文件
if [ -f "$SCRIPT_DIR/.llvm-config" ]; then
    source "$SCRIPT_DIR/.llvm-config"
else
    echo "Error: .llvm-config file not found"
    exit 1
fi

# 检查LLVM是否存在
if [ ! -d "$LLVM_CMAKE_DIR" ]; then
    echo "Error: LLVM not found at $LLVM_PROJECT_DIR"
    echo "Please check the LLVM_PROJECT_DIR in .llvm-config"
    exit 1
fi

# 设置环境变量
export DYLD_LIBRARY_PATH="${LLVM_LIB_DIR}:$DYLD_LIBRARY_PATH"
export LD_LIBRARY_PATH="${LLVM_LIB_DIR}:$LD_LIBRARY_PATH"
export PATH="${LLVM_BIN_DIR}:$PATH"
export LLVM_DIR="$LLVM_CMAKE_DIR"

echo "LLVM Environment Setup"
echo "====================="
echo "LLVM Project:    $LLVM_PROJECT_DIR"
echo "LLVM Build:      $LLVM_BUILD_DIR"
echo "LLVM CMake:      $LLVM_CMAKE_DIR"
echo "LLVM Bin:        $LLVM_BIN_DIR"
echo "LLVM Lib:        $LLVM_LIB_DIR"
echo ""
echo "Environment variables set:"
echo "- DYLD_LIBRARY_PATH: $DYLD_LIBRARY_PATH"
echo "- LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
echo "- PATH: $PATH"
echo "- LLVM_DIR: $LLVM_DIR"
echo ""

# 验证LLVM
if command -v llvm-config &> /dev/null; then
    echo "LLVM Version: $(llvm-config --version)"
else
    echo "Warning: llvm-config not found in PATH"
fi
