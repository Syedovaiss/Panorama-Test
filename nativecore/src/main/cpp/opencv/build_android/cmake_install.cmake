# Install script for directory: /Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv

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

if(CMAKE_INSTALL_COMPONENT STREQUAL "licenses" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/etc/licenses" TYPE FILE RENAME "flatbuffers-LICENSE.txt" FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/3rdparty/flatbuffers/LICENSE.txt")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "samples" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/samples" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/CMakeFiles/install/opencv_android/samples/build.gradle")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "samples" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/samples/gradle/wrapper" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/opencv_android/gradle/wrapper/gradle-wrapper.properties")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "samples" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/samples/gradle/wrapper" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/platforms/android/gradle-wrapper/gradle/wrapper/gradle-wrapper.jar")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "samples" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/samples/." TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/platforms/android/gradle-wrapper/gradlew.bat")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "samples" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/samples/." TYPE FILE PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ GROUP_EXECUTE WORLD_READ WORLD_EXECUTE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/platforms/android/gradle-wrapper/gradlew")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "samples" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/samples/." TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/platforms/android/gradle-wrapper/gradle.properties")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "samples" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/samples" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/CMakeFiles/install/opencv_android/settings.gradle")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "licenses" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/etc/licenses" TYPE FILE RENAME "ade-LICENSE" FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/ade/ade-0.1.2e/LICENSE")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/cvconfig.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/include/opencv2" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/opencv2/opencv_modules.hpp")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/unix-install/OpenCV.mk")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/unix-install/OpenCV-arm64-v8a.mk")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/jni/abi-arm64-v8a/OpenCVModules.cmake")
    file(DIFFERENT _cmake_export_file_changed FILES
         "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/jni/abi-arm64-v8a/OpenCVModules.cmake"
         "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/CMakeFiles/Export/dfc1c36b0b5e57cb58990e6992b52a24/OpenCVModules.cmake")
    if(_cmake_export_file_changed)
      file(GLOB _cmake_old_config_files "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/jni/abi-arm64-v8a/OpenCVModules-*.cmake")
      if(_cmake_old_config_files)
        string(REPLACE ";" ", " _cmake_old_config_files_text "${_cmake_old_config_files}")
        message(STATUS "Old export file \"$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/sdk/native/jni/abi-arm64-v8a/OpenCVModules.cmake\" will be replaced.  Removing files [${_cmake_old_config_files_text}].")
        unset(_cmake_old_config_files_text)
        file(REMOVE ${_cmake_old_config_files})
      endif()
      unset(_cmake_old_config_files)
    endif()
    unset(_cmake_export_file_changed)
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/abi-arm64-v8a" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/CMakeFiles/Export/dfc1c36b0b5e57cb58990e6992b52a24/OpenCVModules.cmake")
  if(CMAKE_INSTALL_CONFIG_NAME MATCHES "^([Rr][Ee][Ll][Ee][Aa][Ss][Ee])$")
    file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/abi-arm64-v8a" TYPE FILE FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/CMakeFiles/Export/dfc1c36b0b5e57cb58990e6992b52a24/OpenCVModules-release.cmake")
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni/abi-arm64-v8a" TYPE FILE FILES
    "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/unix-install/OpenCVConfig-version.cmake"
    "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/unix-install/abi-arm64-v8a/OpenCVConfig.cmake"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/native/jni" TYPE FILE FILES
    "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/unix-install/OpenCVConfig-version.cmake"
    "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/unix-install/OpenCVConfig.cmake"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "libs" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/." TYPE FILE PERMISSIONS OWNER_READ OWNER_WRITE GROUP_READ WORLD_READ FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/LICENSE")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "libs" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/." TYPE FILE PERMISSIONS OWNER_READ OWNER_WRITE GROUP_READ WORLD_READ FILES "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/platforms/android/README.android")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/sdk/etc" TYPE FILE FILES
    "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/platforms/scripts/valgrind.supp"
    "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/platforms/scripts/valgrind_3rdparty.supp"
    )
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for each subdirectory.
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/cpufeatures/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/libjpeg-turbo/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/libtiff/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/libwebp/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/openjpeg/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/libpng/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/openexr/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/protobuf/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/hal/carotene/hal/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/hal/kleidicv/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/3rdparty/ittnotify/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/include/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/calib3d/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/core/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/dnn/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/features2d/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/flann/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/gapi/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/highgui/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/imgcodecs/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/imgproc/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/java/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/js/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/ml/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/objc/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/objdetect/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/photo/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/python/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/stitching/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/ts/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/video/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/videoio/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/.firstpass/world/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/core/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/flann/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/imgproc/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/ml/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/photo/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/dnn/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/features2d/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/imgcodecs/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/videoio/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/calib3d/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/highgui/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/objdetect/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/stitching/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/video/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/gapi/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/java_bindings_generator/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/js_bindings_generator/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/objc_bindings_generator/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/modules/java/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/doc/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/data/cmake_install.cmake")
  include("/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/samples/cmake_install.cmake")

endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
if(CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/install_local_manifest.txt"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()
if(CMAKE_INSTALL_COMPONENT)
  if(CMAKE_INSTALL_COMPONENT MATCHES "^[a-zA-Z0-9_.+-]+$")
    set(CMAKE_INSTALL_MANIFEST "install_manifest_${CMAKE_INSTALL_COMPONENT}.txt")
  else()
    string(MD5 CMAKE_INST_COMP_HASH "${CMAKE_INSTALL_COMPONENT}")
    set(CMAKE_INSTALL_MANIFEST "install_manifest_${CMAKE_INST_COMP_HASH}.txt")
    unset(CMAKE_INST_COMP_HASH)
  endif()
else()
  set(CMAKE_INSTALL_MANIFEST "install_manifest.txt")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "/Users/syedovaisakhtar/AndroidStudioProjects/Panorama/nativecore/src/main/cpp/opencv/build_android/${CMAKE_INSTALL_MANIFEST}"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()
