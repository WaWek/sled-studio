#include <jni.h>
#include <cstdint>
extern "C" JNIEXPORT jlong JNICALL
Java_os_sled_studio_NativeBridge_nativeVersion(JNIEnv*, jobject) { return 1; }
extern "C" JNIEXPORT jlong JNICALL
Java_os_sled_studio_NativeBridge_nativeBfmmlaTest(JNIEnv*, jobject) { return 42; }