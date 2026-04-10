cmake_minimum_required(VERSION 3.10)
project(light_runtime)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

enable_language(ASM)

# 包含头文件目录
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# 添加 C++ 源文件
file(GLOB SRC_FILES ${CMAKE_CURRENT_SOURCE_DIR}/src/*.cpp)
# 单独添加汇编文件
set(ASM_FILE 
  ${CMAKE_CURRENT_SOURCE_DIR}/src/stackunwinder-asm.S
  ${CMAKE_CURRENT_SOURCE_DIR}/src/yieldpoint-asm.S
)

add_library(light SHARED ${SRC_FILES} ${ASM_FILE})

# 添加测试用例
add_executable(test_stackunwinder tests/test_stackunwinder.cpp)
target_link_libraries(test_stackunwinder light)

add_executable(test_forEachRefField tests/test_forEachRefField.cpp)
target_link_libraries(test_forEachRefField light)
