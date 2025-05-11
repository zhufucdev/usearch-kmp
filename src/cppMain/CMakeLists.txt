cmake_minimum_required(VERSION 3.27)

project(usearch-bridge VERSION 1.0.0 LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 11)
set(USEARCH_USE_FP16LIB ON)
include(FetchContent)

FetchContent_Declare(vendor GIT_REPOSITORY https://github.com/unum-cloud/usearch.git GIT_TAG v2.17.7)
FetchContent_MakeAvailable(vendor)

if (BUILD_SHARED_LIBS MATCHES ON)
    set(JAVA_INCLUDE_PATH "$ENV{JAVA_HOME}/include")

    find_package(Java REQUIRED)
    find_package(JNI)

    include_directories(${JNI_INCLUDE_DIRS} ${PROJECT_SOURCE_DIR}/bridging)
    add_library(ksearch SHARED
            lib.cpp lib.h
            jexceptions.h jexceptions.cpp
            jbridge.cpp)
    target_link_libraries(ksearch PRIVATE usearch)
    add_dependencies(ksearch usearch)
endif ()
if (BUILD_STATIC_LIBS MATCHES ON)
    add_library(ksearch STATIC
            lib.cpp lib.h)

    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        target_compile_options(ksearch PRIVATE -static-libstdc++)
        target_link_options(ksearch PRIVATE -static-libstdc++)
    elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        target_compile_options(ksearch PRIVATE -static)
        target_link_options(ksearch PRIVATE -static)
    endif ()

    target_link_libraries(ksearch PRIVATE usearch)
    add_dependencies(ksearch usearch)
endif ()

add_executable(ksearch_test lib.cpp lib.h main.cpp)
target_link_libraries(ksearch_test PRIVATE usearch)