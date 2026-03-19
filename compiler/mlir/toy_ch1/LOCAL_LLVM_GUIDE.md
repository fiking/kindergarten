# 本地LLVM编译指南

## 目录结构

```
toy_ch1/
├── .llvm-config           # LLVM路径配置文件
├── setup-llvm.sh          # LLVM环境设置脚本
├── CMakeLists.txt         # CMake配置（已配置本地LLVM）
├── Makefile               # Make配置（已配置本地LLVM）
├── build.sh               # 构建脚本（已配置本地LLVM）
├── main.cpp               # 编译器源代码
└── build/                 # 构建目录
```

## LLVM配置文件

### .llvm-config

该文件定义本地LLVM源码的路径：
```bash
LLVM_PROJECT_DIR=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project
LLVM_BUILD_DIR=${LLVM_PROJECT_DIR}/build
LLVM_CMAKE_DIR=${LLVM_BUILD_DIR}/lib/cmake/llvm
```

**修改路径**：如果你的LLVM在不同位置，编辑此文件
```bash
nano .llvm-config
```

## 环境配置

### 方法1：使用环境脚本（推荐）

```bash
# 设置LLVM环境变量
source setup-llvm.sh

# 然后构建和运行
./build.sh
./build/compiler
```

### 方法2：手动设置环境变量

```bash
export DYLD_LIBRARY_PATH=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib:$DYLD_LIBRARY_PATH
export LD_LIBRARY_PATH=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib:$LD_LIBRARY_PATH
export PATH=/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/bin:$PATH

./build.sh
./build/compiler
```

## 构建方法

### 快速构建

```bash
./build.sh
```

### CMake构建

```bash
mkdir -p build && cd build
cmake ..
cmake --build .
cd ..
./build/compiler
```

### Make构建

```bash
make build && make run
```

## 验证LLVM配置

### 检查LLVM是否可用

```bash
source setup-llvm.sh
llvm-config --version
llvm-config --libs core
```

### 手动检查路径

```bash
# 检查CMake配置
ls /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm

# 检查二进制工具
ls /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/bin/llvm-config

# 检查库
ls /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/libLLVMCore.a
```

## 故障排除

### 问题：CMake找不到LLVM

**解决方案**：
1. 检查 .llvm-config 中的路径
2. 确保 LLVM 已编译：
   ```bash
   cd /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build
   cmake --build . --parallel 8
   ```
3. 验证CMake文件：
   ```bash
   ls /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/lib/cmake/llvm/LLVMConfig.cmake
   ```

### 问题：运行时找不到库

**解决方案**：
```bash
# 设置库路径
source setup-llvm.sh
./build/compiler
```

### 问题：llvm-config命令不可用

**解决方案**：
```bash
# 使用完整路径
/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project/build/bin/llvm-config --version

# 或设置环境变量
source setup-llvm.sh
llvm-config --version
```

## 高级配置

### 自定义LLVM路径

编辑 CMakeLists.txt：
```cmake
set(LOCAL_LLVM_PROJECT_DIR "/your/custom/path/llvm-project")
```

编辑 build.sh：
```bash
LOCAL_LLVM_PROJECT="/your/custom/path/llvm-project"
```

编辑 Makefile：
```makefile
LOCAL_LLVM_PROJECT := /your/custom/path/llvm-project
```

### 使用系统LLVM（备用）

如果需要使用系统安装的LLVM而不是本地LLVM：

```bash
# macOS - 使用Homebrew LLVM
export LLVM_DIR=/usr/local/opt/llvm/lib/cmake/llvm

# Linux
export LLVM_DIR=/usr/lib/llvm-14/lib/cmake/llvm

cd build
cmake ..
cmake --build .
```

## 清理和重新构建

```bash
# 清理构建目录
make clean

# 或手动删除
rm -rf build build_makefile

# 然后重新构建
./build.sh
```

## 使用本地LLVM开发

完整的开发流程：

```bash
# 1. 设置环境
source setup-llvm.sh

# 2. 首次构建
./build.sh

# 3. 运行编译器
./build/compiler

# 4. 修改代码后重新构建
cd build
cmake --build .
cd ..
./build/compiler

# 5. 清理（可选）
make clean
```

## 相关文件说明

| 文件 | 用途 |
|------|------|
| `.llvm-config` | 定义LLVM路径和配置 |
| `setup-llvm.sh` | 一键设置LLVM环境变量 |
| `CMakeLists.txt` | CMake构建配置（已配置本地LLVM） |
| `build.sh` | 便捷构建脚本 |
| `Makefile` | Make构建配置 |
| `main.cpp` | 编译器源代码 |

## 参考链接

- [LLVM项目源码](file:///Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project)
- [LLVM官方文档](https://llvm.org/)
- [LLVM CMake指南](https://llvm.org/docs/CMake/)
