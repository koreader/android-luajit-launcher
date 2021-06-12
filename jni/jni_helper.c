#include <jni.h>
#include "jni_helper.h"

bool has_permission(struct android_app* app) {
    JNIEnv* env;
    JavaVM* vm = app->activity->vm;
    int status = (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6);
    if ((status == JNI_OK) || ((status == JNI_EDETACHED)
        && ((*vm)->AttachCurrentThread(vm, &env, NULL) == 0)))
    {
        jclass clazz = (*env)->GetObjectClass(env, app->activity->clazz);
        jmethodID method = (*env)->GetMethodID(env, clazz, "hasRequiredPermissions", "()Z");
        bool ok = ((*env)->CallBooleanMethod(env, app->activity->clazz, method) == JNI_TRUE);
        (*vm)->DetachCurrentThread(vm);
        return ok;
    } else {
        return false;
    }
}

void crash_report(struct android_app* app) {
    JNIEnv* env;
    JavaVM* vm = app->activity->vm;
    int status = (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6);
    if ((status == JNI_OK) || ((status == JNI_EDETACHED)
        && ((*vm)->AttachCurrentThread(vm, &env, NULL) == 0)))
    {
        jclass clazz = (*env)->GetObjectClass(env, app->activity->clazz);
        jmethodID method = (*env)->GetMethodID(env, clazz, "onNativeCrash", "()V");
        (*env)->CallVoidMethod(env, app->activity->clazz, method);
        (*vm)->DetachCurrentThread(vm);
    }
}
