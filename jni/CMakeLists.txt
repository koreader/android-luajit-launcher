cmake_minimum_required(VERSION 3.16.3)
project(android-luajit-launcher LANGUAGES C CXX)

find_package(PkgConfig REQUIRED)
pkg_check_modules(LuaJIT luajit REQUIRED IMPORTED_TARGET)
pkg_check_modules(libarchive libarchive REQUIRED IMPORTED_TARGET)

set(THREADS_PREFER_PTHREAD_FLAG TRUE)
find_package(Threads REQUIRED)
if(NOT Threads_FOUND OR NOT CMAKE_USE_PTHREADS_INIT)
    message(FATAL_ERROR "pthread library not found!")
endif()

add_library(7z)
set_target_properties(7z PROPERTIES CXX_VISIBILITY_PRESET hidden)
set_target_properties(7z PROPERTIES VISIBILITY_INLINES_HIDDEN TRUE)
target_link_libraries(7z PRIVATE PkgConfig::libarchive android log)
target_sources(7z PRIVATE 7z/unarchive.cpp)

add_library(ioctl SHARED)
set_target_properties(ioctl PROPERTIES CXX_VISIBILITY_PRESET hidden)
set_target_properties(ioctl PROPERTIES VISIBILITY_INLINES_HIDDEN TRUE)
target_sources(ioctl PRIVATE ioctl/ioctl.cpp)

add_library(luajit-launcher SHARED)
set_target_properties(luajit-launcher PROPERTIES C_VISIBILITY_PRESET hidden)
target_include_directories(luajit-launcher PUBLIC android_native_app_glue)
target_link_libraries(luajit-launcher PRIVATE PkgConfig::LuaJIT Threads::Threads android log)
target_sources(luajit-launcher PRIVATE android_native_app_glue/android_native_app_glue.c jni_helper.c main.c)

install(TARGETS 7z)
install(TARGETS ioctl)
install(TARGETS luajit-launcher)
