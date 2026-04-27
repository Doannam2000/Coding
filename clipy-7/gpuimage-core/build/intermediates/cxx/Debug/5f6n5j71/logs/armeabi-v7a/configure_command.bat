@echo off
"C:\\Users\\NaN\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HD:\\Code\\BotVibeProject\\clipy-7\\gpuimage-core\\src\\main\\cpp" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=26" ^
  "-DANDROID_PLATFORM=android-26" ^
  "-DANDROID_ABI=armeabi-v7a" ^
  "-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a" ^
  "-DANDROID_NDK=C:\\Users\\NaN\\AppData\\Local\\Android\\Sdk\\ndk\\28.2.13676358" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\NaN\\AppData\\Local\\Android\\Sdk\\ndk\\28.2.13676358" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\NaN\\AppData\\Local\\Android\\Sdk\\ndk\\28.2.13676358\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\NaN\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=D:\\Code\\BotVibeProject\\clipy-7\\gpuimage-core\\build\\intermediates\\cxx\\Debug\\5f6n5j71\\obj\\armeabi-v7a" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=D:\\Code\\BotVibeProject\\clipy-7\\gpuimage-core\\build\\intermediates\\cxx\\Debug\\5f6n5j71\\obj\\armeabi-v7a" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BD:\\Code\\BotVibeProject\\clipy-7\\gpuimage-core\\.cxx\\Debug\\5f6n5j71\\armeabi-v7a" ^
  -GNinja
