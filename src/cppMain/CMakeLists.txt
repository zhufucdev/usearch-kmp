cmake_minimum_required(VERSION 3.27)

project(usearch-bridge VERSION 1.0.0 LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 11)
include(FetchContent)

FetchContent_Declare(vendor GIT_REPOSITORY https://github.com/unum-cloud/usearch.git GIT_TAG v2.17.7)
FetchContent_MakeAvailable(vendor)

include_directories(${vendor_SOURCE_DIR}/include ${vendor_SOURCE_DIR}/fp16/include)

if (BUILD_SHARED_LIBS MATCHES ON)
    add_library(ksearch SHARED lib.cpp lib.h)
endif ()
if (BUILD_STATIC_LIBS MATCHES ON)
    add_library(ksearch STATIC lib.cpp lib.h)
endif ()

if (CMAKE_SYSTEM_PROCESSOR EQUAL arm)
    target_compile_definitions(ksearch PUBLIC USEARCH_DEFINED_ARM)
endif ()