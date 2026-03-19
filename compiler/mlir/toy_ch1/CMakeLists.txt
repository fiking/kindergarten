cmake_minimum_required(VERSION 3.10)
project(SimpleLLVMCompiler)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 本地LLVM源码路径
set(LOCAL_LLVM_PROJECT_DIR "/Users/bytedance/code/ArtificialIntelligence/compiler/llvm-project")

# 如果未指定LLVM_DIR，使用本地LLVM
if(NOT LLVM_DIR)
    set(LLVM_DIR "${LOCAL_LLVM_PROJECT_DIR}/build/lib/cmake/llvm")
    message(STATUS "Using local LLVM from: ${LOCAL_LLVM_PROJECT_DIR}")
endif()

# 查找LLVM
find_package(LLVM REQUIRED CONFIG)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")

include_directories(${LLVM_INCLUDE_DIRS})
add_definitions(${LLVM_DEFINITIONS})

# 获取LLVM库和编译标志
llvm_map_components_to_libnames(llvm_libs core)

# 可执行文件
add_executable(compiler main.cpp)

# 链接LLVM库和系统库
target_link_libraries(compiler ${llvm_libs} pthread dl m)
