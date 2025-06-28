cmake_minimum_required(VERSION 3.10)
project(light_runtime)

add_executable(light allocator.cpp)

# 如有其他源文件，可在此处添加
# add_executable(light allocator.cpp other.cpp)

# 如需包含头文件目录，可添加如下内容
# target_include_directories(light PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
