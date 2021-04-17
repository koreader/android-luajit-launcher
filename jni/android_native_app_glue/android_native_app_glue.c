/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include <jni.h>

#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>

#include <android/log.h>

#include "android_native_app_glue.h"
#include "../logger.h"

#define  TAG "[NativeGlue]"

#define FIFO_NAME "alooper.fifo"

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOGGER_NAME, __VA_ARGS__))
#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, LOGGER_NAME, __VA_ARGS__))

#ifndef NDEBUG
#  define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOGGER_NAME, __VA_ARGS__))
#else
#  define LOGD(...) ((void)0)
#endif

static void free_saved_state(struct android_app* android_app) {
    pthread_mutex_lock(&android_app->mutex);
    if (android_app->savedState != NULL) {
        free(android_app->savedState);
        android_app->savedState = NULL;
        android_app->savedStateSize = 0;
    }
    pthread_mutex_unlock(&android_app->mutex);
}

int8_t android_app_read_cmd(struct android_app* android_app) {
    int8_t cmd;
    if (read(android_app->msgread, &cmd, sizeof(cmd)) != sizeof(cmd)) {
        // NOTE: We drain the pipe in one batch in ffi/input_android.lua,
        //       meaning we *expect* the -1 return to detect EAGAIN,
        //       so this is just noise.
        if (errno != EAGAIN) {
            LOGE("%s: Failed to read command pipe: %s", TAG, strerror(errno));
        }
        return -1;
    }
    if (cmd == APP_CMD_SAVE_STATE) {
        free_saved_state(android_app);
    }
    return cmd;
}

/*static void print_cur_config(struct android_app* android_app) {
    char lang[2], country[2];
    AConfiguration_getLanguage(android_app->config, lang);
    AConfiguration_getCountry(android_app->config, country);

    LOGD("%s: Config: mcc=%d mnc=%d lang=%c%c cnt=%c%c orien=%d touch=%d dens=%d "
            "keys=%d nav=%d keysHid=%d navHid=%d sdk=%d size=%d long=%d "
            "modetype=%d modenight=%d", TAG,
            AConfiguration_getMcc(android_app->config),
            AConfiguration_getMnc(android_app->config),
            lang[0], lang[1], country[0], country[1],
            AConfiguration_getOrientation(android_app->config),
            AConfiguration_getTouchscreen(android_app->config),
            AConfiguration_getDensity(android_app->config),
            AConfiguration_getKeyboard(android_app->config),
            AConfiguration_getNavigation(android_app->config),
            AConfiguration_getKeysHidden(android_app->config),
            AConfiguration_getNavHidden(android_app->config),
            AConfiguration_getSdkVersion(android_app->config),
            AConfiguration_getScreenSize(android_app->config),
            AConfiguration_getScreenLong(android_app->config),
            AConfiguration_getUiModeType(android_app->config),
            AConfiguration_getUiModeNight(android_app->config));
}*/

void android_app_pre_exec_cmd(struct android_app* android_app, int8_t cmd) {
    switch (cmd) {
        case APP_CMD_INPUT_CHANGED:
            LOGD("%s: APP_CMD_INPUT_CHANGED", TAG);
            pthread_mutex_lock(&android_app->mutex);
            if (android_app->inputQueue != NULL) {
                AInputQueue_detachLooper(android_app->inputQueue);
            }
            android_app->inputQueue = android_app->pendingInputQueue;
            if (android_app->inputQueue != NULL) {
                LOGD("%s: Attaching input queue to looper", TAG);
                AInputQueue_attachLooper(android_app->inputQueue,
                        android_app->looper, LOOPER_ID_INPUT, NULL,
                        &android_app->inputPollSource);
            }
            pthread_cond_broadcast(&android_app->cond);
            pthread_mutex_unlock(&android_app->mutex);
            break;

        case APP_CMD_INIT_WINDOW:
            LOGD("%s: APP_CMD_INIT_WINDOW", TAG);
            pthread_mutex_lock(&android_app->mutex);
            android_app->window = android_app->pendingWindow;
            pthread_cond_broadcast(&android_app->cond);
            pthread_mutex_unlock(&android_app->mutex);
            break;

        case APP_CMD_TERM_WINDOW:
            LOGD("%s: APP_CMD_TERM_WINDOW", TAG);
            pthread_cond_broadcast(&android_app->cond);
            break;

        case APP_CMD_RESUME:
        case APP_CMD_START:
        case APP_CMD_PAUSE:
        case APP_CMD_STOP:
            LOGD("%s: activityState=%d", TAG, cmd);
            pthread_mutex_lock(&android_app->mutex);
            android_app->activityState = cmd;
            pthread_cond_broadcast(&android_app->cond);
            pthread_mutex_unlock(&android_app->mutex);
            break;

        case APP_CMD_CONFIG_CHANGED:
            LOGD("%s: APP_CMD_CONFIG_CHANGED", TAG);
            AConfiguration_fromAssetManager(android_app->config,
                    android_app->activity->assetManager);
            //print_cur_config(android_app);
            break;

        case APP_CMD_DESTROY:
            LOGD("%s: APP_CMD_DESTROY", TAG);
            android_app->destroyRequested = 1;
            break;
    }
}

void android_app_post_exec_cmd(struct android_app* android_app, int8_t cmd) {
    switch (cmd) {
        case APP_CMD_TERM_WINDOW:
            LOGD("%s: Post APP_CMD_TERM_WINDOW", TAG);
            pthread_mutex_lock(&android_app->mutex);
            android_app->window = NULL;
            pthread_cond_broadcast(&android_app->cond);
            pthread_mutex_unlock(&android_app->mutex);
            break;

        case APP_CMD_SAVE_STATE:
            LOGD("%s: Post APP_CMD_SAVE_STATE", TAG);
            pthread_mutex_lock(&android_app->mutex);
            android_app->stateSaved = 1;
            pthread_cond_broadcast(&android_app->cond);
            pthread_mutex_unlock(&android_app->mutex);
            break;

        case APP_CMD_RESUME:
            LOGD("%s: Post APP_CMD_RESUME", TAG);
            free_saved_state(android_app);
            break;
    }
}

void app_dummy() {
}

static void android_app_destroy(struct android_app* android_app) {
    LOGD("%s: android_app_destroy!", TAG);
    free_saved_state(android_app);
    pthread_mutex_lock(&android_app->mutex);
    if (android_app->inputQueue != NULL) {
        AInputQueue_detachLooper(android_app->inputQueue);
    }
    AConfiguration_delete(android_app->config);
    android_app->destroyed = 1;
    pthread_cond_broadcast(&android_app->cond);
    pthread_mutex_unlock(&android_app->mutex);
    // Can't touch android_app object after this.
}

static void process_input(struct android_app* app, struct android_poll_source* source) {
    AInputEvent* event = NULL;
    while (AInputQueue_getEvent(app->inputQueue, &event) >= 0) {
        LOGD("%s: New input event: type=%d", TAG, AInputEvent_getType(event));
        if (AInputQueue_preDispatchEvent(app->inputQueue, event)) {
            continue;
        }
        int32_t handled = 0;
        if (app->onInputEvent != NULL) handled = app->onInputEvent(app, event);
        AInputQueue_finishEvent(app->inputQueue, event, handled);
    }
}

static void process_cmd(struct android_app* app, struct android_poll_source* source) {
    int8_t cmd = android_app_read_cmd(app);
    android_app_pre_exec_cmd(app, cmd);
    if (app->onAppCmd != NULL) app->onAppCmd(app, cmd);
    android_app_post_exec_cmd(app, cmd);
}

static void* android_app_entry(void* param) {
    struct android_app* android_app = (struct android_app*)param;

    android_app->config = AConfiguration_new();
    AConfiguration_fromAssetManager(android_app->config, android_app->activity->assetManager);

    //print_cur_config(android_app);

    android_app->cmdPollSource.id = LOOPER_ID_MAIN;
    android_app->cmdPollSource.app = android_app;
    android_app->cmdPollSource.process = process_cmd;
    android_app->inputPollSource.id = LOOPER_ID_INPUT;
    android_app->inputPollSource.app = android_app;
    android_app->inputPollSource.process = process_input;

    ALooper* looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    ALooper_addFd(looper, android_app->msgread, LOOPER_ID_MAIN, ALOOPER_EVENT_INPUT, NULL,
            &android_app->cmdPollSource);
    android_app->looper = looper;

    pthread_mutex_lock(&android_app->mutex);
    android_app->running = 1;
    pthread_cond_broadcast(&android_app->cond);
    pthread_mutex_unlock(&android_app->mutex);

    /* from https://github.com/koreader/android-luajit-launcher/pull/294

    We use a named pipe to push events to our main loop running in LuaJIT.
    Here we create the file, open it and register it as a poll source if everything went well.

    Its state is available as part of the android ffi module.
    */
    const char* fifo_path = android_app->activity->internalDataPath;
    size_t len = strlen(fifo_path) + strlen(FIFO_NAME) + 2U; // +1 for "/" and +1 for NULL
    char fifo_file[len];
    snprintf(fifo_file, len, "%s/%s", fifo_path, FIFO_NAME);

    if (mkfifo(fifo_file, 0666) == -1) {
        if (errno == EEXIST) {
            LOGV("%s: User FIFO already exists at `%s`", TAG, fifo_file);
        } else {
            LOGE("%s: Failed to create user FIFO at `%s`: %s", TAG, fifo_file, strerror(errno));
        }
    } else {
        LOGV("%s: User FIFO created at `%s`", TAG, fifo_file);
    }

    int fifo_fd = open(fifo_file, O_RDWR | O_NONBLOCK | O_CLOEXEC);
    if (fifo_fd  == -1) {
        LOGE("%s: Failed to open user FIFO at `%s`: %s", TAG, fifo_file, strerror(errno));
    } else {
        ALooper_addFd(looper, fifo_fd, LOOPER_ID_USER, ALOOPER_EVENT_INPUT, NULL, &android_app->cmdPollSource);
    }

    android_main(android_app);

    // clean up fifo
    if (fifo_fd != -1) {
        ALooper_removeFd(looper, fifo_fd);
        close(fifo_fd);
    }

    android_app_destroy(android_app);
    return NULL;
}

// --------------------------------------------------------------------
// Native activity interaction (called from main thread)
// --------------------------------------------------------------------

static struct android_app* android_app_create(ANativeActivity* activity,
                                              void* savedState, size_t savedStateSize) {
    struct android_app* android_app = calloc(1, sizeof(struct android_app));
    android_app->activity = activity;

    pthread_mutex_init(&android_app->mutex, NULL);
    pthread_cond_init(&android_app->cond, NULL);

    if (savedState != NULL) {
        android_app->savedState = malloc(savedStateSize);
        android_app->savedStateSize = savedStateSize;
        memcpy(android_app->savedState, savedState, savedStateSize);
    }

    int msgpipe[2];
    if (pipe(msgpipe)) {
        LOGE("%s: Failed to create command pipe: %s", TAG, strerror(errno));
        return NULL;
    }
    // Because pipe2 has only been available as a bionic wrapper hilariously recently...
    // (https://android.googlesource.com/platform/bionic/+/1fad5283a07e87b3ae28f4a2dd6943d600c2926b)
    for (size_t i = 0U; i < 2U; i++) {
        int flflags = fcntl(msgpipe[i], F_GETFL);
        fcntl(msgpipe[i], F_SETFL, flflags | O_NONBLOCK);
        int fdflags = fcntl(msgpipe[i], F_GETFD);
        fcntl(msgpipe[i], F_SETFD, fdflags | FD_CLOEXEC);
    }

    android_app->msgread = msgpipe[0];
    android_app->msgwrite = msgpipe[1];

    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_create(&android_app->thread, &attr, android_app_entry, android_app);

    // Wait for thread to start.
    pthread_mutex_lock(&android_app->mutex);
    while (!android_app->running) {
        pthread_cond_wait(&android_app->cond, &android_app->mutex);
    }
    pthread_mutex_unlock(&android_app->mutex);

    return android_app;
}

static void android_app_write_cmd(struct android_app* android_app, int8_t cmd) {
    if (write(android_app->msgwrite, &cmd, sizeof(cmd)) != sizeof(cmd)) {
        LOGE("%s: Failed to write to command pipe: %s", TAG, strerror(errno));
    }
}

static void android_app_set_input(struct android_app* android_app, AInputQueue* inputQueue) {
    pthread_mutex_lock(&android_app->mutex);
    android_app->pendingInputQueue = inputQueue;
    android_app_write_cmd(android_app, APP_CMD_INPUT_CHANGED);
    while (android_app->inputQueue != android_app->pendingInputQueue) {
        pthread_cond_wait(&android_app->cond, &android_app->mutex);
    }
    pthread_mutex_unlock(&android_app->mutex);
}

static void android_app_set_window(struct android_app* android_app, ANativeWindow* window) {
    pthread_mutex_lock(&android_app->mutex);
    if (android_app->pendingWindow != NULL) {
        android_app_write_cmd(android_app, APP_CMD_TERM_WINDOW);
    }
    android_app->pendingWindow = window;
    if (window != NULL) {
        android_app_write_cmd(android_app, APP_CMD_INIT_WINDOW);
    }
    while (android_app->window != android_app->pendingWindow) {
        pthread_cond_wait(&android_app->cond, &android_app->mutex);
    }
    pthread_mutex_unlock(&android_app->mutex);
}

static void android_app_set_activity_state(struct android_app* android_app, int8_t cmd) {
    pthread_mutex_lock(&android_app->mutex);
    android_app_write_cmd(android_app, cmd);
    while (android_app->activityState != cmd) {
        pthread_cond_wait(&android_app->cond, &android_app->mutex);
    }
    pthread_mutex_unlock(&android_app->mutex);
}

static void android_app_free(struct android_app* android_app) {
    pthread_mutex_lock(&android_app->mutex);
    android_app_write_cmd(android_app, APP_CMD_DESTROY);
    while (!android_app->destroyed) {
        pthread_cond_wait(&android_app->cond, &android_app->mutex);
    }
    pthread_mutex_unlock(&android_app->mutex);

    close(android_app->msgread);
    close(android_app->msgwrite);
    pthread_cond_destroy(&android_app->cond);
    pthread_mutex_destroy(&android_app->mutex);
    free(android_app);
}

static struct android_app* ToApp(ANativeActivity* activity) {
    return (struct android_app*) activity->instance;
}

static void onDestroy(ANativeActivity* activity) {
    LOGD("%s: Destroy: %p", TAG, activity);
    android_app_free(ToApp(activity));
}

static void onStart(ANativeActivity* activity) {
    LOGD("%s: Start: %p", TAG, activity);
    android_app_set_activity_state(ToApp(activity), APP_CMD_START);
}

static void onResume(ANativeActivity* activity) {
    LOGD("%s: Resume: %p", TAG, activity);
    android_app_set_activity_state(ToApp(activity), APP_CMD_RESUME);
}

static void* onSaveInstanceState(ANativeActivity* activity, size_t* outLen) {
    LOGD("%s: SaveInstanceState: %p", TAG, activity);

    struct android_app* android_app = ToApp(activity);
    void* savedState = NULL;
    pthread_mutex_lock(&android_app->mutex);
    android_app->stateSaved = 0;
    android_app_write_cmd(android_app, APP_CMD_SAVE_STATE);
    while (!android_app->stateSaved) {
        pthread_cond_wait(&android_app->cond, &android_app->mutex);
    }

    if (android_app->savedState != NULL) {
        savedState = android_app->savedState;
        *outLen = android_app->savedStateSize;
        android_app->savedState = NULL;
        android_app->savedStateSize = 0;
    }

    pthread_mutex_unlock(&android_app->mutex);

    return savedState;
}

static void onPause(ANativeActivity* activity) {
    LOGD("%s: Pause: %p", TAG, activity);
    android_app_set_activity_state(ToApp(activity), APP_CMD_PAUSE);
}

static void onStop(ANativeActivity* activity) {
    LOGD("%s: Stop: %p", TAG, activity);
    android_app_set_activity_state(ToApp(activity), APP_CMD_STOP);
}

static void onConfigurationChanged(ANativeActivity* activity) {
    LOGD("%s: ConfigurationChanged: %p", TAG, activity);
    android_app_write_cmd(ToApp(activity), APP_CMD_CONFIG_CHANGED);
}

static void onContentRectChanged(ANativeActivity* activity, const ARect* r) {
    LOGD("%s: ContentRectChanged: l=%d,t=%d,r=%d,b=%d", TAG, r->left, r->top, r->right, r->bottom);
    struct android_app* android_app = ToApp(activity);
    pthread_mutex_lock(&android_app->mutex);
    android_app->contentRect = *r;
    pthread_mutex_unlock(&android_app->mutex);
    android_app_write_cmd(ToApp(activity), APP_CMD_CONTENT_RECT_CHANGED);
}

static void onLowMemory(ANativeActivity* activity) {
    LOGD("%s: LowMemory: %p", TAG, activity);
    android_app_write_cmd(ToApp(activity), APP_CMD_LOW_MEMORY);
}

static void onWindowFocusChanged(ANativeActivity* activity, int focused) {
    LOGD("%s: WindowFocusChanged: %p -- %d", TAG, activity, focused);
    android_app_write_cmd(ToApp(activity), focused ? APP_CMD_GAINED_FOCUS : APP_CMD_LOST_FOCUS);
}

static void onNativeWindowCreated(ANativeActivity* activity, ANativeWindow* window) {
    LOGD("%s: NativeWindowCreated: %p -- %p", TAG, activity, window);
    android_app_set_window(ToApp(activity), window);
}

static void onNativeWindowDestroyed(ANativeActivity* activity, ANativeWindow* window) {
    LOGD("%s: NativeWindowDestroyed: %p -- %p", TAG, activity, window);
    android_app_set_window(ToApp(activity), NULL);
}

static void onNativeWindowRedrawNeeded(ANativeActivity* activity, ANativeWindow* window) {
    LOGD("%s: NativeWindowRedrawNeeded: %p -- %p", TAG, activity, window);
    android_app_write_cmd(ToApp(activity), APP_CMD_WINDOW_REDRAW_NEEDED);
}

static void onNativeWindowResized(ANativeActivity* activity, ANativeWindow* window) {
    LOGD("%s: NativeWindowResized: %p -- %p", TAG, activity, window);
    android_app_write_cmd(ToApp(activity), APP_CMD_WINDOW_RESIZED);
}

static void onInputQueueCreated(ANativeActivity* activity, AInputQueue* queue) {
    LOGD("%s: InputQueueCreated: %p -- %p", TAG, activity, queue);
    android_app_set_input(ToApp(activity), queue);
}

static void onInputQueueDestroyed(ANativeActivity* activity, AInputQueue* queue) {
    LOGD("%s: InputQueueDestroyed: %p -- %p", TAG, activity, queue);
    android_app_set_input(ToApp(activity), NULL);
}

JNIEXPORT
void ANativeActivity_onCreate(ANativeActivity* activity, void* savedState, size_t savedStateSize) {
    LOGD("%s: Creating: %p", TAG, activity);

    activity->callbacks->onConfigurationChanged = onConfigurationChanged;
    activity->callbacks->onContentRectChanged = onContentRectChanged;
    activity->callbacks->onDestroy = onDestroy;
    activity->callbacks->onInputQueueCreated = onInputQueueCreated;
    activity->callbacks->onInputQueueDestroyed = onInputQueueDestroyed;
    activity->callbacks->onLowMemory = onLowMemory;
    activity->callbacks->onNativeWindowCreated = onNativeWindowCreated;
    activity->callbacks->onNativeWindowDestroyed = onNativeWindowDestroyed;
    activity->callbacks->onNativeWindowRedrawNeeded = onNativeWindowRedrawNeeded;
    activity->callbacks->onNativeWindowResized = onNativeWindowResized;
    activity->callbacks->onPause = onPause;
    activity->callbacks->onResume = onResume;
    activity->callbacks->onSaveInstanceState = onSaveInstanceState;
    activity->callbacks->onStart = onStart;
    activity->callbacks->onStop = onStop;
    activity->callbacks->onWindowFocusChanged = onWindowFocusChanged;

    activity->instance = android_app_create(activity, savedState, savedStateSize);
}
