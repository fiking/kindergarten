# Simple LLVM Compiler - a + b

一个使用LLVM接口实现的简单编译器，可以编译和执行 `a + b` 这样的表达式。

## 项目结构

```
.
├── CMakeLists.txt      # CMake 编译配置
├── main.cpp            # 编译器源代码
└── README.md           # 本文件
```

## 编译器组成

### 1. 词法分析器 (Lexer)
- 将输入字符串分解为 token
- 支持的 token 类型：
  - 标识符 (identifiers): `a`, `b`, `x` 等
  - 数字 (numbers): `1`, `2.5` 等
  - 操作符 (operators): `+`, `-`, `*`, `/`
  - 括号 (parentheses): `(`, `)`

### 2. 语法分析器 (Parser)
- 使用递归下降解析法构建抽象语法树 (AST)
- 支持的语法：
  - primary: 标识符、数字或带括号的表达式
  - 二元操作：支持 `+`, `-`, `*`, `/`
  - 优先级处理：`*`, `/` 优先于 `+`, `-`

### 3. 代码生成器 (CodeGen)
- 从 AST 生成 LLVM IR 代码
- 使用 LLVM IRBuilder API 构建中间表示
- 支持浮点数运算

### 4. JIT 执行
- 使用 LLVM 的执行引擎对生成的代码进行即时编译和执行

## 构建步骤

### 前置条件
- 本地 LLVM 源码已编译
  - 默认路径：`/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project`
- CMake 3.10 或更高版本
- C++14 或更高的编译器

### 快速构建（推荐）

使用提供的构建脚本，自动使用本地 LLVM：

```bash
chmod +x build.sh
./build.sh
```

### 使用 CMake 构建

```bash
mkdir -p build
cd build

# 使用本地LLVM的默认配置
cmake ..

# 或者指定本地LLVM路径
cmake -DLLVM_DIR=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm ..

# 编译
cmake --build .
```

### 使用 Makefile 构建

```bash
make build
```

## 运行编译器

编译完成后，运行可执行文件：

```bash
./build/compiler
```

或使用 Makefile：

```bash
make run
```

### 预期输出

编译器将：
1. 解析表达式 `a + b`
2. 生成 LLVM IR 代码
3. 使用 JIT 编译执行代码
4. 打印结果

示例输出：
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

## 支持的表达式

编译器可以编译以下表达式（修改 `main()` 中的 `Input` 变量）：

- 简单加法：`a + b`
- 四则运算：`a + b * 2`、`(a + b) * 2`、`a - b / 2`
- 多变量：`x + y + z`
- 含数字：`a + 5`、`2.5 * b`

## 代码说明

### 主要类

| 类名 | 功能 |
|------|------|
| `Lexer` | 词法分析 - 将输入分解为 token |
| `Parser` | 语法分析 - 构建 AST |
| `ExprAST` 及子类 | 抽象语法树节点 |
| `CodeGen` | 代码生成 - 从 AST 生成 LLVM IR |

### 主要函数流程

```
输入字符串 "a + b"
    ↓
词法分析 (Lexer)
    ↓
语法分析 (Parser) → 生成 AST
    ↓
代码生成 (CodeGen) → 生成 LLVM IR
    ↓
验证模块 (verifyModule)
    ↓
JIT 编译执行
    ↓
输出结果
```

## 扩展功能

可以通过修改代码来扩展编译器的功能：

1. **添加更多操作符**：在 `BinaryExprAST` 的 codegen 中添加 case
2. **添加函数调用**：扩展 Parser 和 AST
3. **添加条件表达式**：支持 if-else 语句
4. **编译到文件**：使用 LLVM 的代码生成后端编译到 `.o` 或 `.ll` 文件
5. **命令行输入**：从命令行参数读取表达式

## 参考资源

- [LLVM 官方文档](https://llvm.org/docs/)
- [LLVM Language Reference Manual](https://llvm.org/docs/LangRef/)
- [Kaleidoscope Tutorial](https://llvm.org/docs/tutorial/MyFirstLanguageFrontend/)

## 常见问题

### 编译时找不到本地 LLVM

**问题**：构建脚本报错 "Local LLVM not found"

**解决方案**：

1. 检查 LLVM 源码路径：
```bash
ls /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm
```

2. 如果路径不存在，确保 LLVM 已编译：
```bash
cd /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build
cmake --build .
```

3. 如果 LLVM 在不同的位置，修改 CMakeLists.txt、build.sh 和 Makefile 中的路径

### 修改本地 LLVM 路径

如果你的 LLVM 源码在不同位置，需要修改以下文件：

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

### 动态库链接错误

**问题**：运行时找不到 LLVM 动态库

**解决方案**：
```bash
# macOS
export DYLD_LIBRARY_PATH=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib:$DYLD_LIBRARY_PATH

# Linux
export LD_LIBRARY_PATH=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib:$LD_LIBRARY_PATH

./build/compiler
```

## 许可证

MIT License
