#!/bin/bash

# 一键编译和测试脚本
# 自动处理所有编译步骤

echo "=========================================="
echo "LLVM 编译器 - 一键编译脚本"
echo "=========================================="
echo ""

# 设置LLVM环境变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_LLVM_PROJECT="/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project"
LLVM_CMAKE_DIR="${LOCAL_LLVM_PROJECT}/build/lib/cmake/llvm"

# 检查LLVM
if [ ! -d "$LLVM_CMAKE_DIR" ]; then
    echo "❌ 错误：本地LLVM未找到"
    echo "   路径: $LLVM_CMAKE_DIR"
    exit 1
fi

echo "✓ 本地LLVM已找到: $LOCAL_LLVM_PROJECT"
echo ""

# 清理旧的构建
echo "1️⃣  清理旧的构建文件..."
rm -rf "$SCRIPT_DIR/build"
echo "   ✓ 清理完成"
echo ""

# 创建构建目录
echo "2️⃣  创建构建目录..."
mkdir -p "$SCRIPT_DIR/build"
cd "$SCRIPT_DIR/build" || exit 1
echo "   ✓ 构建目录已创建"
echo ""

# 运行CMake
echo "3️⃣  运行CMake配置..."
if cmake -DLLVM_DIR="$LLVM_CMAKE_DIR" ..; then
    echo "   ✓ CMake配置成功"
else
    echo "   ❌ CMake配置失败"
    exit 1
fi
echo ""

# 编译项目
echo "4️⃣  编译项目..."
if cmake --build . --parallel $(nproc 2>/dev/null || sysctl -n hw.ncpu); then
    echo "   ✓ 编译成功"
else
    echo "   ❌ 编译失败"
    exit 1
fi
echo ""

# 检查可执行文件
if [ -f "compiler" ]; then
    echo "✅ 编译完成！"
    echo ""
    echo "📊 构建统计："
    echo "   - 可执行文件: $(pwd)/compiler"
    echo "   - 大小: $(du -h compiler | cut -f1)"
    echo ""
    echo "🚀 运行编译器："
    echo "   $(pwd)/compiler"
    echo ""
else
    echo "❌ 编译后未找到可执行文件"
    exit 1
fi
