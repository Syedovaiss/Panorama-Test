cmake_minimum_required(VERSION 3.22)

project(nativecore LANGUAGES CXX)

# ------------------------------------------------------------------
# Native library
# ------------------------------------------------------------------
add_library(
        nativecore
        SHARED
        nativecore.cpp
)

set_target_properties(
        nativecore
        PROPERTIES
        CXX_STANDARD 20
        CXX_STANDARD_REQUIRED YES
        CXX_EXTENSIONS NO
)

# ------------------------------------------------------------------
# ✅ OpenCV headers
# Path MUST contain 'opencv2/'
# Example:
# app/src/main/cpp/include/opencv/include/opencv2/...
# ------------------------------------------------------------------
target_include_directories(
        nativecore
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# ------------------------------------------------------------------
# ✅ Path to OpenCV native .so files
#
# Example structure:
# app/src/main/jniLibs/arm64-v8a/libopencv_core.so
# app/src/main/jniLibs/arm64-v8a/libopencv_stitching.so
# ------------------------------------------------------------------
set(OPENCV_LIB_DIR
    ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}
)

# ------------------------------------------------------------------
# ✅ Import OpenCV modules (ONE BY ONE)
# ------------------------------------------------------------------
add_library(opencv_core SHARED IMPORTED)
set_target_properties(opencv_core PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_DIR}/libopencv_core.so)

add_library(opencv_imgproc SHARED IMPORTED)
set_target_properties(opencv_imgproc PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_DIR}/libopencv_imgproc.so)

add_library(opencv_imgcodecs SHARED IMPORTED)
set_target_properties(opencv_imgcodecs PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_DIR}/libopencv_imgcodecs.so)

add_library(opencv_features2d SHARED IMPORTED)
set_target_properties(opencv_features2d PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_DIR}/libopencv_features2d.so)

add_library(opencv_flann SHARED IMPORTED)
set_target_properties(opencv_flann PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_DIR}/libopencv_flann.so)

add_library(opencv_calib3d SHARED IMPORTED)
set_target_properties(opencv_calib3d PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_DIR}/libopencv_calib3d.so)

add_library(opencv_photo SHARED IMPORTED)
set_target_properties(opencv_photo PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_DIR}/libopencv_photo.so)

add_library(opencv_stitching SHARED IMPORTED)
set_target_properties(opencv_stitching PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_DIR}/libopencv_stitching.so)

# ------------------------------------------------------------------
# ✅ Android system libraries
# ------------------------------------------------------------------
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

# ------------------------------------------------------------------
# ✅ Link everything (ORDER MATTERS)
# ------------------------------------------------------------------
target_link_libraries(
        nativecore
        opencv_stitching
        opencv_photo
        opencv_calib3d
        opencv_features2d
        opencv_flann
        opencv_imgcodecs
        opencv_imgproc
        opencv_core
        ${jnigraphics-lib}
        ${log-lib}
)