# 简单LLVM编译器 - 快速开始指南

## 配置（已预配置为本地LLVM）

项目已配置为使用本地LLVM源码：
```
/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project
```

## 快速开始（3步）

### 1. 使用构建脚本编译

```bash
chmod +x build.sh
./build.sh
```

### 2. 运行编译器

```bash
./build/compiler
```

### 3. 查看输出

程序会输出：
- 原始表达式
- 生成的 LLVM IR 代码
- 执行结果（8.0）

## 修改表达式

打开 `main.cpp`，找到以下代码行（约第217行）：

```cpp
std::string Input = "a + b";
```

修改为你想要的表达式，例如：

- `"a + b"`         → 结果：8.0（5.0 + 3.0）
- `"a - b"`         → 结果：2.0（5.0 - 3.0）
- `"a * b"`         → 结果：15.0（5.0 * 3.0）
- `"a / b"`         → 结果：1.666...（5.0 / 3.0）
- `"a + b * 2"`     → 结果：11.0（5.0 + 3.0*2）
- `"(a + b) * 2"`   → 结果：16.0（(5.0 + 3.0)*2）

## 修改变量值

在 `main.cpp` 中，找到以下代码（约第224-227行）：

```cpp
std::map<std::string, double> vars;
vars["a"] = 5.0;
vars["b"] = 3.0;
```

修改变量值即可，例如：

```cpp
vars["a"] = 10.0;
vars["b"] = 20.0;
```

然后重新编译和运行。

## 完整的编译和测试流程

```bash
# 直接编译（自动使用配置的本地LLVM）
./build.sh

# 运行
./build/compiler

# 修改表达式后，重新编译
cd build
cmake --build .
cd ..
./build/compiler
```

## 使用Makefile编译

```bash
# 编译并运行
make build && make run

# 或者分别执行
make build
make run

# 清理
make clean
```

## 示例输出

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

## 故障排除

### 错误："Local LLVM not found"

**解决方案**：确保LLVM源码已编译
```bash
# 检查LLVM是否存在
ls /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm

# 如果不存在，确保LLVM已构建：
cd /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build
cmake --build .
```

### 修改LLVM源码路径

如果你的本地LLVM在不同的位置，修改以下文件中的路径：

**CMakeLists.txt**：
```cmake
set(LOCAL_LLVM_PROJECT_DIR "/your/path/to/llvm-project")
```

**build.sh**：
```bash
LOCAL_LLVM_PROJECT="/your/path/to/llvm-project"
```

**Makefile**：
```makefile
LOCAL_LLVM_PROJECT := /your/path/to/llvm-project
```

## 下一步

- 了解生成的 LLVM IR 代码
- 修改编译器支持更复杂的表达式
- 学习 LLVM 优化 pass
- 将编译结果输出为可执行文件或目标文件

更详细的信息请查看 README.md
