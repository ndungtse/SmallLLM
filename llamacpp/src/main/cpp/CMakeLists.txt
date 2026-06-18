# Builds libsmollm.so: a thin JNI wrapper around llama.cpp for running GGUF models on-device.
#
# llama.cpp is vendored as a git submodule at `llama.cpp/` (see llamacpp/README.md). This file is
# only invoked by Gradle once that submodule exists (the externalNativeBuild block in
# build.gradle.kts is guarded on its presence), so the module still builds Kotlin-only without it.

cmake_minimum_required(VERSION 3.22.1)
project(smollm)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Build llama.cpp library-only (no examples/tests/tools/server on device), statically linked into
# libsmollm.so so we ship a single shared object.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

add_library(smollm SHARED
        smollm.cpp
        LLMInference.cpp)

target_link_libraries(smollm
        llama
        android
        log)
