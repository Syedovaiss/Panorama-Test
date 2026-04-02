cmake_minimum_required(VERSION 3.18)
project(nativecore)

set(OPENCV_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../../../../open-cv/native)
set(OPENCV_INCLUDE_DIR ${OPENCV_ROOT}/jni/include)

add_library(nativecore SHARED nativecore.cpp)

target_include_directories(nativecore PRIVATE ${OPENCV_INCLUDE_DIR})

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

# Use the shared library provided in the open-cv module
add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION
    ${OPENCV_ROOT}/libs/${ANDROID_ABI}/libopencv_java4.so)

target_link_libraries(
    nativecore
    opencv_java4
    ${log-lib}
    ${jnigraphics-lib}
)
