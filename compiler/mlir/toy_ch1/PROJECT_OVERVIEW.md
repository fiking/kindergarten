# 项目概览

## 📋 项目简介

这是一个使用LLVM接口实现的**简单编译器**，能够编译和执行 `a + b` 这样的算术表达式。

该项目展示了一个完整的编译器架构，包括：
- **词法分析（Lexing）** - 将源代码分解为tokens
- **语法分析（Parsing）** - 构建抽象语法树（AST）
- **代码生成（Code Generation）** - 生成LLVM中间表示（IR）
- **优化与执行** - 通过JIT编译执行生成的代码

## 📁 项目文件结构

```
toy_ch1/
├── main.cpp              # 主编译器实现
├── CMakeLists.txt        # CMake构建配置
├── Makefile              # Make构建配置（可选）
├── build.sh              # 快速构建脚本
├── README.md             # 完整文档
├── QUICKSTART.md         # 快速开始指南
├── EXAMPLES.md           # 扩展功能示例
├── PROJECT_OVERVIEW.md   # 本文件
└── build/                # 构建目录
    └── compiler          # 编译后的可执行文件


```

## 🏗️ 编译器架构

### 架构图

```
输入字符串
    │
    ▼
┌─────────────────────────────────┐
│   Lexical Analysis (词法分析)    │
│  "a+b" → [ID(a), OP(+), ID(b)]  │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│   Syntax Analysis (语法分析)     │
│   Tokens → Abstract Syntax Tree │
│        （AST）                   │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│   Code Generation (代码生成)     │
│   AST → LLVM IR                 │
│   %addtmp = fadd double 5, 3    │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│   Module Verification (模块验证)  │
│   验证LLVM IR的正确性            │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│   JIT Compilation & Execution   │
│   (JIT编译和执行)                │
│   运行代码，返回结果              │
└─────────────────────────────────┘
    │
    ▼
  结果：8.0
```

## 🔧 核心组件说明

### 1. Lexer（词法分析器）

**功能**：将输入字符串分解为tokens

**支持的token类型**：
- 标识符（Identifiers）: `a`, `b`, `x`
- 数字（Numbers）: `1`, `2.5`, `3.14`
- 操作符（Operators）: `+`, `-`, `*`, `/`
- 括号（Parentheses）: `(`, `)`

**流程**：
```
输入：" a + b "
     ▼
跳过空白
     ▼
读取标识符 'a'
     ▼
跳过空白
     ▼
读取操作符 '+'
     ▼
...以此类推
     ▼
输出：Token序列
```

### 2. Parser（语法分析器）

**功能**：根据tokens构建抽象语法树（AST）

**使用递归下降解析法**：
```
Expression    ::= Term (('+' | '-') Term)*
Term          ::= Factor (('*' | '/') Factor)*
Factor        ::= Number | Identifier | '(' Expression ')'
```

**构建的AST示例**：
```
对于表达式 "a + b"：

      BinaryExpr(+)
      /           \
  Variable(a)    Variable(b)

对于表达式 "a + b * 2"：

      BinaryExpr(+)
      /                \
  Variable(a)    BinaryExpr(*)
                 /           \
            Variable(b)    Number(2)
```

### 3. CodeGen（代码生成器）

**功能**：将AST转换为LLVM IR代码

**LLVM IR示例**：
```llvm
define double @compute() {
entry:
  %addtmp = fadd double 5.000000e+00, 3.000000e+00
  ret double %addtmp
}
```

**生成的指令**：
- `fadd`：浮点加法
- `fsub`：浮点减法
- `fmul`：浮点乘法
- `fdiv`：浮点除法
- `ret`：返回值

### 4. JIT执行引擎

**功能**：即时编译（JIT）LLVM IR为机器代码并执行

**步骤**：
1. 创建ExecutionEngine
2. 获取函数地址
3. 调用函数指针
4. 返回结果

## 📊 数据流示例

### 编译 "a + b"（其中 a=5.0, b=3.0）

```
┌─────────────────────────────────────────────────────────────┐
│ 输入: "a + b"                                               │
│ 变量: a = 5.0, b = 3.0                                      │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ Lexer 输出:                                                 │
│ [tok_identifier("a"), tok_plus(+), tok_identifier("b")]    │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ Parser 输出 (AST):                                          │
│     BinaryExprAST(+)                                        │
│     /            \                                          │
│ VariableExprAST(a) VariableExprAST(b)                       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ CodeGen 输出 (LLVM IR):                                     │
│ define double @compute() {                                  │
│ entry:                                                      │
│   %addtmp = fadd double 5.0, 3.0                            │
│   ret double %addtmp                                        │
│ }                                                           │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ JIT 执行:                                                   │
│ compute() → 8.0                                             │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 构建项目

**使用CMake（推荐）**：
```bash
mkdir -p build
cd build
cmake ..
cmake --build .
cd ..
```

**使用Makefile**：
```bash
make build
```

**使用构建脚本**：
```bash
chmod +x build.sh
./build.sh
```

### 运行编译器

```bash
./build/compiler
```

### 输出示例

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

## 📚 核心类和方法

| 类 | 方法/功能 | 说明 |
|-----|---------|------|
| `Lexer` | `getTok()` | 获取下一个token |
| `Parser` | `parseExpression()` | 解析表达式 |
| `Parser` | `parseBinOpRHS()` | 解析二元操作符右侧 |
| `CodeGen` | `codegen()` | 从AST生成LLVM IR |
| `ExprAST` | 虚基类 | 所有AST节点的基类 |
| `NumberExprAST` | - | 数字常数节点 |
| `VariableExprAST` | - | 变量节点 |
| `BinaryExprAST` | - | 二元操作节点 |

## 🎯 支持的表达式

编译器可以编译以下表达式（修改 main.cpp 中的 `Input` 变量）：

- `a + b` → 8.0
- `a - b` → 2.0
- `a * b` → 15.0
- `a / b` → 1.666...
- `a * 2` → 10.0
- `(a + b) * 2` → 16.0
- `a + b * 2` → 11.0（因为 * 优先级高于 +）

## 🔍 运算符优先级

| 优先级 | 操作符 |
|--------|--------|
| 1 | `*`, `/` |
| 2 | `+`, `-` |

（高优先级的操作符先执行）

## 🛠️ 编译系统

### 依赖

- LLVM 12.0 或更高版本
- CMake 3.10 或更高版本
- C++14 或更高的编译器
- macOS / Linux / Windows

### 支持的构建工具

- CMake（推荐）
- Make
- 自定义构建脚本

## 📖 文档

| 文件 | 内容 |
|------|------|
| `README.md` | 完整文档和详细说明 |
| `QUICKSTART.md` | 快速开始指南和常见问题 |
| `EXAMPLES.md` | 扩展功能示例代码 |
| `PROJECT_OVERVIEW.md` | 本文件，项目概览 |

## 🌟 主要特性

✅ 完整的编译器前端（Lexer + Parser + Codegen）
✅ LLVM IR 生成
✅ JIT 编译执行
✅ 多种构建选项
✅ 详细的文档和示例
✅ 支持扩展和优化

## 🚀 下一步

1. **理解代码** - 阅读 main.cpp 中的注释
2. **修改表达式** - 尝试编译不同的表达式
3. **查看LLVM IR** - 理解生成的中间代码
4. **扩展功能** - 参考 EXAMPLES.md 添加新特性
5. **优化代码** - 使用LLVM的优化pass

## 📚 参考资源

- [LLVM 官方文档](https://llvm.org/)
- [Kaleidoscope 教程](https://llvm.org/docs/tutorial/MyFirstLanguageFrontend/)
- [LLVM Language Reference](https://llvm.org/docs/LangRef/)
- [LLVM API Documentation](https://llvm.org/doxygen/)

## 💡 学习目标

通过本项目，你将学到：

1. 如何实现一个**词法分析器**
2. 如何实现一个**语法分析器**
3. 如何使用**LLVM API**生成中间代码
4. 如何使用**JIT编译**执行代码
5. 编译器的**完整工作流程**

## 📝 许可证

MIT License

---

**开始编译吧！** 🎉
