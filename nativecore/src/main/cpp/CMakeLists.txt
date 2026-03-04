cmake_minimum_required(VERSION 3.18)
project(nativecore)

add_library(nativecore SHARED nativecore.cpp)

# Include OpenCV headers
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/opencv/include)

# Android log
find_library(log-lib log)

# Import libopencv_java4.so
add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION
    ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so
)

# Link libraries
target_link_libraries(
    nativecore
    opencv_java4
    ${log-lib}
)