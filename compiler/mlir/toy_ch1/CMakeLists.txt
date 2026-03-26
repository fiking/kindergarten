cmake_minimum_required(VERSION 3.10)
project(SimpleLLVMCompiler)

# 设置 Debug 编译模式
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Debug)
endif()

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(NO_RTTI "-fno-rtti")
add_definitions(${NO_RTTI})

# 本地LLVM源码路径
set(LOCAL_LLVM_PROJECT_DIR "/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project")

# 如果未指定LLVM_DIR，使用本地LLVM
if(NOT LLVM_DIR)
    set(LLVM_DIR "${LOCAL_LLVM_PROJECT_DIR}/build/lib/cmake/llvm")
    message(STATUS "Using local LLVM from: ${LOCAL_LLVM_PROJECT_DIR}")
endif()

# 如果未指定MLIR_DIR，使用本地MLIR
if(NOT MLIR_DIR)
    set(MLIR_DIR "${LOCAL_LLVM_PROJECT_DIR}/build/lib/cmake/mlir")
    message(STATUS "Using local MLIR from: ${LOCAL_LLVM_PROJECT_DIR}")
endif()

# 查找LLVM和MLIR
find_package(LLVM REQUIRED CONFIG)
find_package(MLIR REQUIRED CONFIG)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Found MLIR ${MLIR_PACKAGE_VERSION}")

list(APPEND CMAKE_MODULE_PATH "${MLIR_CMAKE_DIR}")
list(APPEND CMAKE_MODULE_PATH "${LLVM_CMAKE_DIR}")
include(TableGen)
include(AddLLVM)
include(AddMLIR)

# 获取LLVM库和编译标志
llvm_map_components_to_libnames(llvm_libs core)

# 添加项目include目录
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include ${LLVM_INCLUDE_DIRS} ${MLIR_INCLUDE_DIRS})
include_directories(${CMAKE_CURRENT_BINARY_DIR}/include/)
include_directories(${CMAKE_CURRENT_BINARY_DIR})
add_definitions(${LLVM_DEFINITIONS} ${MLIR_DEFINITIONS})

# 添加 include 子目录（包含 TableGen 构建规则）
add_subdirectory(include)

set(LLVM_TARGET_DEFINITIONS ToyCombine.td)
mlir_tablegen(ToyCombine.inc -gen-rewriters)
add_public_tablegen_target(ToyCombineIncGen)

# 可执行文件
add_executable(compiler main.cpp AST.cpp MLIRGen.cpp Dialect.cpp ToyCombine.cpp ShapeInferencePass.cpp)

# 添加对 TableGen 生成文件的依赖
add_dependencies(compiler ToyOpsIncGen ToyCombineIncGen ToyShapeInferenceInterfaceIncGen)

# 链接LLVM库、MLIR库和系统库
target_link_libraries(compiler PRIVATE ${llvm_libs} MLIRParser MLIRPass MLIRTransforms)
