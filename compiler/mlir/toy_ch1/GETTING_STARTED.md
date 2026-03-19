# 使用本地LLVM编译 - 完整指南

## 📋 项目配置状态

你的编译器项目已完全配置为使用本地LLVM源码！

```
本地LLVM路径：
/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project

项目路径：
/Users/bytedance/code/kindergarten/compiler/mlir/toy_ch1
```

## 🚀 快速开始（3种方法）

### 方法1：一键编译（✅ 推荐）

```bash
cd /Users/bytedance/code/kindergarten/compiler/mlir/toy_ch1
chmod +x quick-build.sh
./quick-build.sh
```

### 方法2：使用build.sh脚本

```bash
cd /Users/bytedance/code/kindergarten/compiler/mlir/toy_ch1
chmod +x build.sh
./build.sh
```

### 方法3：手动编译

```bash
cd /Users/bytedance/code/kindergarten/compiler/mlir/toy_ch1

# 设置环境变量
source setup-llvm.sh

# 清理并重新构建
rm -rf build
mkdir -p build
cd build

# 配置
LLVM_CMAKE_DIR="/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm"
cmake -DLLVM_DIR="$LLVM_CMAKE_DIR" ..

# 编译
cmake --build . --parallel $(sysctl -n hw.ncpu)

# 运行
./compiler
```

## 📁 项目文件说明

| 文件 | 用途 |
|------|------|
| `main.cpp` | 编译器源代码 |
| `CMakeLists.txt` | CMake配置（已配置本地LLVM） |
| `Makefile` | Make构建配置 |
| `build.sh` | 构建脚本 |
| `quick-build.sh` | 一键编译脚本 |
| `setup-llvm.sh` | LLVM环境设置 |
| `verify-config.sh` | 配置验证脚本 |
| `.llvm-config` | LLVM路径配置 |
| `LOCAL_LLVM_GUIDE.md` | 本地LLVM详细指南 |
| `README.md` | 项目说明文档 |
| `QUICKSTART.md` | 快速开始指南 |

## ✅ 配置验证

运行验证脚本检查所有配置是否正确：

```bash
cd /Users/bytedance/code/kindergarten/compiler/mlir/toy_ch1
chmod +x verify-config.sh
./verify-config.sh
```

## 🔧 编译命令详解

### 使用CMake

```bash
# 配置（首次需要）
cmake -DLLVM_DIR="/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm" ..

# 编译
cmake --build . --parallel 8

# 运行
./compiler
```

### 使用Make

```bash
# 编译
make build

# 运行
make run

# 清理
make clean
```

## 🎯 常见问题

### Q1: 编译失败，CMake找不到LLVM

**A:** 检查LLVM cmake配置是否存在：
```bash
ls /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm
```

如果不存在，确保LLVM已编译：
```bash
cd /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build
cmake --build . --parallel 8
```

### Q2: 运行时出现"找不到库"的错误

**A:** 设置库路径环境变量：
```bash
export DYLD_LIBRARY_PATH=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib:$DYLD_LIBRARY_PATH
./build/compiler
```

或使用环境脚本：
```bash
source setup-llvm.sh
./build/compiler
```

### Q3: 如何修改编译的表达式？

**A:** 编辑 `main.cpp` 第217行：
```cpp
std::string Input = "a + b";  // 修改这里
```

支持的表达式：
- `a + b`
- `a - b`
- `a * b`
- `a / b`
- `(a + b) * 2`
- `a + b * 2`

然后重新编译：
```bash
cd build
cmake --build .
./compiler
```

### Q4: 如何修改变量值？

**A:** 编辑 `main.cpp` 第224-227行：
```cpp
std::map<std::string, double> vars;
vars["a"] = 5.0;   // 修改这里
vars["b"] = 3.0;   // 修改这里
```

## 📊 编译结果示例

```
========== Simple LLVM Compiler ==========
Compiling expression: a + b

========== Generated LLVM IR ==========
; ModuleID = 'simple_compiler'
source_filename = "simple_compiler"

define double @compute() {
entry:
  %addtmp = fadd double 5.000000e+00, 3.000000e+00
  ret double %addtmp
}


========== Execution Result ==========
Variables: a = 5.0, b = 3.0
Result: a + b = 8

```

## 🔄 工作流程

```
编辑代码 (main.cpp)
    ↓
运行 quick-build.sh
    ↓
CMake 配置
    ↓
编译
    ↓
自动运行编译器
    ↓
查看结果
```

## 🧪 测试编译器

### 测试1：基本加法
```bash
# main.cpp line 217: std::string Input = "a + b";
./build/compiler
# 预期输出: 8.0
```

### 测试2：乘法
```bash
# main.cpp line 217: std::string Input = "a * b";
./build/compiler
# 预期输出: 15.0
```

### 测试3：混合运算
```bash
# main.cpp line 217: std::string Input = "(a + b) * 2";
./build/compiler
# 预期输出: 16.0
```

## 📚 了解更多

- [LOCAL_LLVM_GUIDE.md](LOCAL_LLVM_GUIDE.md) - 本地LLVM详细指南
- [README.md](README.md) - 完整项目文档
- [QUICKSTART.md](QUICKSTART.md) - 快速开始指南
- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) - 项目架构概览
- [EXAMPLES.md](EXAMPLES.md) - 扩展功能示例

## 🎓 学习路径

1. **理解编译器架构**
   - 阅读 PROJECT_OVERVIEW.md
   - 查看 main.cpp 中的注释

2. **编译项目**
   - 运行 quick-build.sh

3. **修改表达式**
   - 编辑 main.cpp
   - 重新编译

4. **查看LLVM IR**
   - 观察编译器输出

5. **扩展功能**
   - 参考 EXAMPLES.md
   - 实现新功能

## 🔗 相关资源

- [LLVM官方文档](https://llvm.org/)
- [LLVM项目源码](file:///Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project)
- [CMake官方文档](https://cmake.org/)

---

**准备好开始编译了吗？** 🎉

```bash
cd /Users/bytedance/code/kindergarten/compiler/mlir/toy_ch1
./quick-build.sh
```

