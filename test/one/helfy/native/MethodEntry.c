#include <jni.h>

#ifdef _MSC_VER
#include <intrin.h>
#else
#include <x86intrin.h>
#endif

JNIEXPORT jlong JNICALL
Java_one_helfy_MethodEntry_rdtsc(JNIEnv* env, jobject unused) {
    return (jlong)__rdtsc();
}
