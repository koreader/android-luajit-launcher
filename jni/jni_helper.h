#include <stdbool.h>
#include "android_native_app_glue.h"

bool has_permission(struct android_app* app);
void crash_report(struct android_app* app);
