#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include "7zExtractor.h"

JNIEXPORT jint JNICALL
Java_org_koreader_launcher_Assets_extract(JNIEnv *env, __unused jobject obj,
        jobject assetManager, jstring payload, jstring output)
{
    const char *name = env->GetStringUTFChars(payload, nullptr);
    const char *out = env->GetStringUTFChars(output, nullptr);
    jint res = extractAsset(env, assetManager, name, out, nullptr, 0x4000000);
    env->ReleaseStringUTFChars(payload, name);
    env->ReleaseStringUTFChars(output, out);
    return res;
}

#ifdef __cplusplus
}
#endif
