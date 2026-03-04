cmake_minimum_required(VERSION 3.18)
project(nativecore)

# Set OpenCV_DIR to point to the build directory containing OpenCVConfig.cmake
set(OpenCV_DIR ${CMAKE_CURRENT_SOURCE_DIR}/opencv/build_android)
find_package(OpenCV REQUIRED)

add_library(nativecore SHARED nativecore.cpp)

# OpenCV_INCLUDE_DIRS is set by find_package
target_include_directories(nativecore PRIVATE ${OpenCV_INCLUDE_DIRS})

# Android log
find_library(log-lib log)

# Link libraries
# ${OpenCV_LIBS} contains all the necessary OpenCV modules
target_link_libraries(
    nativecore
    ${OpenCV_LIBS}
    ${log-lib}
)