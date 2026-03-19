#!/bin/bash

# 本地LLVM编译器配置检验脚本

echo "=========================================="
echo "LLVM编译器项目配置检验"
echo "=========================================="
echo ""

ERRORS=0
WARNINGS=0

# 检查 .llvm-config 文件
echo "1. 检查配置文件..."
if [ -f ".llvm-config" ]; then
    echo "   ✓ .llvm-config 文件存在"
else
    echo "   ✗ .llvm-config 文件不存在"
    ERRORS=$((ERRORS + 1))
fi

# 检查 setup-llvm.sh 脚本
echo ""
echo "2. 检查环境设置脚本..."
if [ -f "setup-llvm.sh" ]; then
    echo "   ✓ setup-llvm.sh 文件存在"
    if [ -x "setup-llvm.sh" ]; then
        echo "   ✓ setup-llvm.sh 有执行权限"
    else
        echo "   ! setup-llvm.sh 无执行权限（警告）"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo "   ✗ setup-llvm.sh 文件不存在"
    ERRORS=$((ERRORS + 1))
fi

# 检查 build.sh 脚本
echo ""
echo "3. 检查构建脚本..."
if [ -f "build.sh" ]; then
    echo "   ✓ build.sh 文件存在"
else
    echo "   ✗ build.sh 文件不存在"
    ERRORS=$((ERRORS + 1))
fi

# 检查 CMakeLists.txt
echo ""
echo "4. 检查CMake配置..."
if [ -f "CMakeLists.txt" ]; then
    echo "   ✓ CMakeLists.txt 文件存在"
    if grep -q "LOCAL_LLVM_PROJECT_DIR" CMakeLists.txt; then
        echo "   ✓ CMakeLists.txt 已配置本地LLVM"
    else
        echo "   ! CMakeLists.txt 未配置本地LLVM（警告）"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo "   ✗ CMakeLists.txt 文件不存在"
    ERRORS=$((ERRORS + 1))
fi

# 检查 Makefile
echo ""
echo "5. 检查Makefile配置..."
if [ -f "Makefile" ]; then
    echo "   ✓ Makefile 文件存在"
    if grep -q "LOCAL_LLVM_PROJECT" Makefile; then
        echo "   ✓ Makefile 已配置本地LLVM"
    else
        echo "   ! Makefile 未配置本地LLVM（警告）"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo "   ✗ Makefile 文件不存在"
    ERRORS=$((ERRORS + 1))
fi

# 检查 main.cpp
echo ""
echo "6. 检查源代码..."
if [ -f "main.cpp" ]; then
    echo "   ✓ main.cpp 文件存在"
    if [ -s "main.cpp" ]; then
        echo "   ✓ main.cpp 不为空"
    else
        echo "   ✗ main.cpp 文件为空"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo "   ✗ main.cpp 文件不存在"
    ERRORS=$((ERRORS + 1))
fi

# 检查本地LLVM
echo ""
echo "7. 检查本地LLVM..."

LLVM_PROJECT="/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project"

if [ -d "$LLVM_PROJECT" ]; then
    echo "   ✓ LLVM项目目录存在"
else
    echo "   ✗ LLVM项目目录不存在: $LLVM_PROJECT"
    ERRORS=$((ERRORS + 1))
fi

LLVM_BUILD="$LLVM_PROJECT/build"
if [ -d "$LLVM_BUILD" ]; then
    echo "   ✓ LLVM构建目录存在"
else
    echo "   ✗ LLVM构建目录不存在: $LLVM_BUILD"
    ERRORS=$((ERRORS + 1))
fi

LLVM_CMAKE="$LLVM_BUILD/lib/cmake/llvm"
if [ -d "$LLVM_CMAKE" ]; then
    echo "   ✓ LLVM CMake配置存在"
else
    echo "   ✗ LLVM CMake配置不存在: $LLVM_CMAKE"
    ERRORS=$((ERRORS + 1))
fi

LLVM_LIB="$LLVM_BUILD/lib"
if [ -d "$LLVM_LIB" ]; then
    echo "   ✓ LLVM库目录存在"
else
    echo "   ✗ LLVM库目录不存在: $LLVM_LIB"
    ERRORS=$((ERRORS + 1))
fi

# 检查必要的工具
echo ""
echo "8. 检查必要工具..."

if command -v cmake &> /dev/null; then
    echo "   ✓ CMake 已安装 (版本: $(cmake --version | head -n 1))"
else
    echo "   ✗ CMake 未安装"
    ERRORS=$((ERRORS + 1))
fi

if command -v clang++ &> /dev/null; then
    echo "   ✓ clang++ 已安装"
else
    echo "   ! clang++ 未安装（警告，但可能有其他C++编译器）"
    WARNINGS=$((WARNINGS + 1))
fi

if command -v make &> /dev/null; then
    echo "   ✓ Make 已安装"
else
    echo "   ✗ Make 未安装"
    ERRORS=$((ERRORS + 1))
fi

# 检查文档
echo ""
echo "9. 检查文档..."

for doc in README.md QUICKSTART.md LOCAL_LLVM_GUIDE.md EXAMPLES.md PROJECT_OVERVIEW.md; do
    if [ -f "$doc" ]; then
        echo "   ✓ $doc 文件存在"
    else
        echo "   ! $doc 文件不存在（信息）"
    fi
done

# 总结
echo ""
echo "=========================================="
echo "检验结果"
echo "=========================================="

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo "✓ 所有检查通过！项目已准备好编译。"
    echo ""
    echo "下一步："
    echo "1. 运行: chmod +x build.sh setup-llvm.sh"
    echo "2. 运行: ./build.sh"
    echo "3. 运行: ./build/compiler"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo "✓ 所有关键检查通过，有 $WARNINGS 个警告"
    echo "  项目可以编译，但建议解决警告。"
    exit 0
else
    echo "✗ 检查失败：发现 $ERRORS 个错误，$WARNINGS 个警告"
    echo ""
    echo "请修复上述错误后重试。"
    exit 1
fi
