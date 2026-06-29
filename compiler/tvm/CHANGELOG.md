# Changelog

## tvm-0.01

TVM 原型项目首个版本,搭建了符号表达式与 Tensor IR 的基础框架。

### 符号表达式

- `Expr` 基类,支持 `+`/`-`/`*`/`/`/负号` 运算符重载及反向运算
- `ConstExpr`、`BinaryOpExpr`、`UnaryOpExpr`、`Var` 表达式节点
- `NameManager` 自动命名管理器,保证变量名唯一

### 运算符

- 二元运算:`AddOp`、`SubOp`、`MulOp`、`DivOp`、`MaxOp`、`MinOp`
- 规约运算:`reduce_sum`、`reduce_prod`、`reduce_min`、`reduce_max`
- `canonical` 规范化与 `simplify` 表达式化简

### 表达式工具 (`expr_util`)

- `transform`:递归遍历并收集结果
- `visit`:后序遍历应用回调
- `format_str`:表达式转字符串
- `bind`:变量绑定替换
- `simplify`:基于规范化形式的化简

### Tensor IR

- `Tensor`:支持通过 lambda `fcompute` 构造计算图,可选 `shape`
- `TensorReadExpr`:张量索引读取
- `input_tensors()`:收集输入张量(去重保序)
- `infer_input_domains()`:由输出域推断输入张量各维定义域
- `is_rtensor`:判断是否为规约结果张量

### Domain 推断 (`domain`)

- `Range`:一维区间表示
- `RDom`:规约域
- `infer_range`:根据变量域推断表达式值域
- `union_range`:区间合并

### 测试

- 表达式构造、运算符、格式化、变量绑定
- Tensor 构造、输入张量收集、规约
- 域推断(domain infer、range infer)

### 运行

```bash
cd compiler/tvm
./python_test.sh
```
