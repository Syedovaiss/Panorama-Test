# Install script for directory: /Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/install")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Release")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set path to fallback-tool for dependency-resolution.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/Users/syedovaisakhtar/Library/Android/sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-objdump")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "libs" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/libs/arm64-v8a/libopencv_flann.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/libs/arm64-v8a/libopencv_flann.so")
    file(RPATH_CHECK
         FILE "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/libs/arm64-v8a/libopencv_flann.so"
         RPATH "")
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/libs/arm64-v8a" TYPE SHARED_LIBRARY OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/lib/arm64-v8a/libopencv_flann.so")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/libs/arm64-v8a/libopencv_flann.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/libs/arm64-v8a/libopencv_flann.so")
    if(CMAKE_INSTALL_DO_STRIP)
      execute_process(COMMAND "/Users/syedovaisakhtar/Library/Android/sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-strip" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/libs/arm64-v8a/libopencv_flann.so")
    endif()
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann.hpp")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/all_indices.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/allocator.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/any.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/autotuned_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/composite_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/config.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/defines.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/dist.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/dummy.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/dynamic_bitset.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/flann.hpp")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/flann_base.hpp")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/general.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/ground_truth.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/heap.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/hierarchical_clustering_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/index_testing.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/kdtree_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/kdtree_single_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/kmeans_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/linear_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/logger.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/lsh_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/lsh_table.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/matrix.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/miniflann.hpp")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/nn_index.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/object_factory.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/params.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/random.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/result_set.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/sampling.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/saving.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/simplex_downhill.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2/flann" TYPE FILE OPTIONAL FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/flann/include/opencv2/flann/timer.h")
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
if(CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/flann/install_local_manifest.txt"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()
