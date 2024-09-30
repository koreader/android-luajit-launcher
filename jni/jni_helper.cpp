#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include "jni_helper.h"

bool has_permission(struct android_app* app) {
    JNIEnv* env;
    JavaVM* vm = app->activity->vm;
    int status = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if ((status == JNI_OK) || ((status == JNI_EDETACHED)
        && (vm->AttachCurrentThread(&env, NULL) == 0)))
    {
        jclass clazz = env->GetObjectClass(app->activity->clazz);
        jmethodID method = env->GetMethodID(clazz, "hasRequiredPermissions", "()Z");
        bool ok = env->CallBooleanMethod(app->activity->clazz, method) == JNI_TRUE;
        vm->DetachCurrentThread();
        return ok;
    } else {
        return false;
    }
}

void crash_report(struct android_app* app) {
    JNIEnv* env;
    JavaVM* vm = app->activity->vm;
    int status = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if ((status == JNI_OK) || ((status == JNI_EDETACHED)
        && (vm->AttachCurrentThread(&env, NULL) == 0)))
    {
        jclass clazz = env->GetObjectClass(app->activity->clazz);
        jmethodID method = env->GetMethodID(clazz, "onNativeCrash", "()V");
        env->CallVoidMethod(app->activity->clazz, method);
        vm->DetachCurrentThread();
    }
}

#ifdef __cplusplus
}
#endif
