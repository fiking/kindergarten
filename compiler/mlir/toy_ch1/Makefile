.PHONY: all build clean run rebuild help configure

# LLVM Compiler Makefile

# 编译器和标志
CXX := clang++
CXXFLAGS := -std=c++14 -fPIC -Wall -Wextra

# 本地LLVM项目路径配置
LOCAL_LLVM_PROJECT := /Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project
LLVM_CMAKE_DIR := $(LOCAL_LLVM_PROJECT)/build/lib/cmake/llvm

# 获取 LLVM 配置（从本地LLVM）
LLVM_CONFIG := $(LOCAL_LLVM_PROJECT)/build/bin/llvm-config
LLVM_CXXFLAGS := $(shell $(LLVM_CONFIG) --cxxflags)
LLVM_LDFLAGS := $(shell $(LLVM_CONFIG) --ldflags)
LLVM_LIBS := $(shell $(LLVM_CONFIG) --libs core)

# 最终编译和链接标志
FINAL_CXXFLAGS := $(CXXFLAGS) $(LLVM_CXXFLAGS)
FINAL_LDFLAGS := $(LLVM_LDFLAGS)
FINAL_LIBS := $(LLVM_LIBS)

# 输出目录
BUILD_DIR := build_makefile
OBJ_DIR := $(BUILD_DIR)/obj
BIN_DIR := $(BUILD_DIR)/bin

# 源文件和目标文件
SOURCES := main.cpp
OBJECTS := $(addprefix $(OBJ_DIR)/, $(SOURCES:.cpp=.o))
EXECUTABLE := $(BIN_DIR)/compiler

# 默认目标
all: $(EXECUTABLE)

# 创建输出目录
$(OBJ_DIR):
	@mkdir -p $(OBJ_DIR)

$(BIN_DIR):
	@mkdir -p $(BIN_DIR)

# 编译对象文件
$(OBJ_DIR)/%.o: %.cpp | $(OBJ_DIR)
	@echo "Compiling $<..."
	$(CXX) $(FINAL_CXXFLAGS) -c $< -o $@

# 链接可执行文件
$(EXECUTABLE): $(OBJECTS) | $(BIN_DIR)
	@echo "Linking $@..."
	$(CXX) $(FINAL_LDFLAGS) -o $@ $^ $(FINAL_LIBS)
	@echo "Build successful: $@"

# 配置目标
configure:
	@echo "Using local LLVM from: $(LOCAL_LLVM_PROJECT)"
	@echo "LLVM Config: $(LLVM_CONFIG)"
	@echo "LLVM Version: $(shell $(LLVM_CONFIG) --version)"
	@echo "CXXFLAGS: $(FINAL_CXXFLAGS)"

# 构建目标
build: configure $(EXECUTABLE)

# 运行编译器
run: $(EXECUTABLE)
	@$(EXECUTABLE)

# 重新构建
rebuild: clean build

# 清理
clean:
	@echo "Cleaning build directory..."
	@rm -rf $(BUILD_DIR)
	@echo "Clean complete"

# 帮助信息
help:
	@echo "Simple LLVM Compiler - Makefile Targets"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Configuration:"
	@echo "  Local LLVM: $(LOCAL_LLVM_PROJECT)"
	@echo ""
	@echo "Targets:"
	@echo "  all          - Build the compiler (default)"
	@echo "  build        - Build the compiler with configuration"
	@echo "  run          - Run the compiled compiler"
	@echo "  rebuild      - Clean and rebuild"
	@echo "  clean        - Remove build files"
	@echo "  configure    - Show LLVM configuration"
	@echo "  help         - Show this help message"
	@echo ""
	@echo "Example:"
	@echo "  make build && make run"
	@echo "  make rebuild"
	@echo ""
	@echo "To use a different LLVM installation:"
	@echo "  make LOCAL_LLVM_PROJECT=/path/to/llvm-project"
