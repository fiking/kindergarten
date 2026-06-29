# tvm

TVM 原型项目,用于学习和实验 TVM 编译器框架。

## 项目结构

```
.
├── python/tvm/          # TVM 包源码
│   └── __init__.py      # 包入口
├── tests/python/        # 测试
│   └── test_basic.py    # 导入冒烟测试
└── python_test.sh       # 测试运行脚本
```

## 运行测试

在 `compiler/tvm/` 目录下执行:

```bash
./python_test.sh
```

> **注意:** `python_test.sh` 通过绝对路径设置 `PYTHONPATH` 指向 `python/` 目录,需在 `compiler/tvm/` 下运行。若路径与当前机器不符,修改脚本中的 `PYTHONPATH` 即可。
