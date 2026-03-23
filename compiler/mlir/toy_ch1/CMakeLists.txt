cmake_minimum_required(VERSION 3.10)
project(SimpleLLVMCompiler)

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

# 查找LLVM
find_package(LLVM REQUIRED CONFIG)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")

# 查找MLIR
find_package(MLIR REQUIRED CONFIG)

message(STATUS "Found MLIR ${MLIR_PACKAGE_VERSION}")
message(STATUS "Using MLIRConfig.cmake in: ${MLIR_DIR}")

# 添加项目include目录
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include ${LLVM_INCLUDE_DIRS} ${MLIR_INCLUDE_DIRS})
add_definitions(${LLVM_DEFINITIONS} ${MLIR_DEFINITIONS})

# 获取LLVM库和编译标志
llvm_map_components_to_libnames(llvm_libs core)

# 可执行文件
add_executable(compiler main.cpp AST.cpp)

# 链接LLVM库、MLIR库和系统库
target_link_libraries(compiler PRIVATE ${llvm_libs} MLIRIR MLIRSupport pthread)
