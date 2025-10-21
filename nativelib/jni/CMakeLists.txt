cmake_minimum_required(VERSION 3.6.0)
project("player")

set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "-std=c++14 -DANDROID -g -fno-inline -O0 -fno-builtin")
set(CMAKE_C_FLAGS "-w -std=c11 -DANDROID -D_LARGEFILE_SOURCE=1 -g")
set(CMAKE_ASM_FLAGS "${CFLAGS} -x assembler-with-cpp")

set(CMAKE_VERBOSE_MAKEFILE ON)

set(CMAKE_BUILD_TYPE Debug)

set(NATIVE_LIB "player")
#
link_directories(${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/${ANDROID_ABI})
add_library(${NATIVE_LIB} SHARED
        enc/Enc.cpp
        Player.cpp
        native-lib.cpp
)

target_include_directories(${NATIVE_LIB} PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/include
)

target_link_libraries(${NATIVE_LIB}
                avdevice
                avformat
                avcodec
                swscale
                swresample
                avutil

        #        avresample

        android
        log
)