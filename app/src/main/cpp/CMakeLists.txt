cmake_minimum_required(VERSION 3.22.1)
project(native_lib)

add_library(native-lib SHARED native-lib.cpp)

# OpenCV include directory
include_directories(
        ${CMAKE_SOURCE_DIR}/opencv/include
)

# Add OpenCV shared library
add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so
)

# Link
target_link_libraries(
        native-lib
        opencv_java4
        android
        log
)
