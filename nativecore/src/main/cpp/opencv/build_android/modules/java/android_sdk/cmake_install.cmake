# Install script for directory: /Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/modules/java/android_sdk

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

if(CMAKE_INSTALL_COMPONENT STREQUAL "java" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/java" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/opencv_android/opencv/AndroidManifest.xml")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "java" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/java/res/values" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/opencv_android/opencv/res/values/attrs.xml")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "java" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/java/../libcxx_helper" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/opencv_android/opencv/libcxx_helper/CMakeLists.txt")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "java" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/java/../libcxx_helper" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/opencv_android/opencv/libcxx_helper/dummy.cpp")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "java" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/java/.." TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/CMakeFiles/install/opencv_android/opencv/build.gradle")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "java" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/java" TYPE DIRECTORY FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/opencv_android/opencv/src")
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
if(CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/java/android_sdk/install_local_manifest.txt"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()
