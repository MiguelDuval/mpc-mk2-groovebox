cmake_minimum_required(VERSION 3.22)

project(MpcMk2GrooveboxNative LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(MPC_BUILD_TRACKTION "Build Tracktion Engine integration targets" OFF)
option(MPC_BUILD_LINK "Build Ableton Link integration target" OFF)

# Oboe is the first native audio dependency in the bootstrap. Tracktion/JUCE
# and Ableton Link are pinned as submodules but remain disabled in this first
# Android build until their integration boundaries are validated.
add_subdirectory(third_party/oboe EXCLUDE_FROM_ALL)

add_library(mpcgroovebox SHARED
    src/NativeEngine.cpp
    src/MPC/MpcStudioMk2ControlMap.cpp
    src/MPC/MpcStudioMk2LedProtocol.cpp
)

target_include_directories(mpcgroovebox PRIVATE src)

target_link_libraries(mpcgroovebox PRIVATE oboe)

if(ANDROID)
    find_library(log-lib log)
    find_library(android-lib android)
    target_link_libraries(mpcgroovebox PRIVATE ${log-lib} ${android-lib})
endif()

if(MPC_BUILD_TRACKTION)
    # Reuse the parent JUCE targets so Tracktion does not require its nested
    # JUCE checkout for this project.
    add_subdirectory(third_party/JUCE EXCLUDE_FROM_ALL)
    set(TE_ADD_EXAMPLES OFF CACHE BOOL "" FORCE)
    add_subdirectory(third_party/tracktion_engine EXCLUDE_FROM_ALL)
endif()

if(MPC_BUILD_LINK)
    add_subdirectory(third_party/ableton_link EXCLUDE_FROM_ALL)
endif()
