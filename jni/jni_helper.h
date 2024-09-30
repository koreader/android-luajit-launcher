#ifdef __cplusplus
extern "C" {
#else
#include <stdbool.h>
#endif

#include <jni.h>
#include "android_native_app_glue.h"

bool has_permission(struct android_app* app);
void crash_report(struct android_app* app);

#ifdef __cplusplus
}
#endif
