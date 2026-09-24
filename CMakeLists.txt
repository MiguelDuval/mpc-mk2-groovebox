cmake_minimum_required(VERSION 3.22)

project(MpcMk2GrooveboxNative LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(MPC_BUILD_TRACKTION "Build Tracktion Engine integration targets" OFF)
option(MPC_BUILD_LINK "Build Ableton Link integration target" OFF)

add_subdirectory(third_party/oboe EXCLUDE_FROM_ALL)

add_library(mpcgroovebox SHARED
    src/NativeEngine.cpp
    src/Midi/NativeMidi.cpp
    src/MPC/MpcStudioMk2ControlMap.cpp
    src/MPC/MpcStudioMk2LedProtocol.cpp
    src/MPC/Domain/MpcDomain.cpp
)

target_include_directories(mpcgroovebox PRIVATE src)

target_link_libraries(mpcgroovebox PRIVATE oboe)

if(ANDROID)
    find_library(log-lib log)
    find_library(android-lib android)
    target_link_libraries(mpcgroovebox PRIVATE ${log-lib} ${android-lib})
endif()

if(MPC_BUILD_TRACKTION)
    add_subdirectory(third_party/JUCE EXCLUDE_FROM_ALL)
    set(TE_ADD_EXAMPLES OFF CACHE BOOL "" FORCE)
    add_subdirectory(third_party/tracktion_engine EXCLUDE_FROM_ALL)
endif()

if(MPC_BUILD_LINK)
    add_subdirectory(third_party/ableton_link EXCLUDE_FROM_ALL)
endif()
