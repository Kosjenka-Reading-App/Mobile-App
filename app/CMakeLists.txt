# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
message("project directory is: ${PROJECT_SOURCE_DIR}")
set( Visage_LIBS ${PROJECT_SOURCE_DIR}/../visage-sdk/lib/${ANDROID_ABI} )
set( Visage_HEADERS ../visage-sdk/include)

include_directories( ${Visage_HEADERS} )
set(ANDROID_STL "c++_shared")

add_library( libomp SHARED IMPORTED )
add_library( tfplugin SHARED IMPORTED )
add_library( VisageVision SHARED IMPORTED )

set_target_properties( libomp PROPERTIES IMPORTED_LOCATION ${Visage_LIBS}/libomp.so )
set_target_properties( tfplugin PROPERTIES IMPORTED_LOCATION ${Visage_LIBS}/libTFPlugin.so )
set_target_properties( VisageVision PROPERTIES IMPORTED_LOCATION ${Visage_LIBS}/libVisageVision.so )

add_library( VisageAnalyser SHARED IMPORTED )
set_target_properties( VisageAnalyser PROPERTIES
        IMPORTED_LOCATION ${Visage_LIBS}/libVisageAnalyser.so )

add_library( VisageGaze SHARED IMPORTED )
set_target_properties( VisageGaze PROPERTIES
        IMPORTED_LOCATION ${Visage_LIBS}/libVisageGaze.so )

set( Wrapper_SOURCE src/main/jni/AndroidWrapper.cpp
                    src/main/jni/AndroidStreamCapture.cpp
                    src/main/jni/VisageRendering.cpp
                    src/main/jni/AndroidImageCapture.cpp
                    src/main/jni/AndroidCapture.cpp)

add_library( VisageWrapper SHARED ${Wrapper_SOURCE} )

target_link_libraries( VisageWrapper libomp tfplugin VisageVision VisageAnalyser VisageGaze "-lGLESv1_CM -llog -ldl -Wl,--gc-sections" )