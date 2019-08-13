/*
Copyright (c) 2014 Hans-Werner Hilse <software@haveyouseenthiscat.de>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

#include <stdlib.h>
#include <linux/elf.h>

#include <android/log.h>
#include <android/asset_manager.h>

#include "android_native_app_glue.h"
#include "logger.h"

#include "luajit-2.1/lua.h"
#include "luajit-2.1/lualib.h"
#include "luajit-2.1/lauxlib.h"

#define  TAG "[NativeThread]"

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOGGER_NAME, __VA_ARGS__))
#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, LOGGER_NAME, __VA_ARGS__))

#ifndef NDEBUG
#  define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOGGER_NAME, __VA_ARGS__))
#else
#  define LOGD(...) ((void)0)
#endif

#define  LOADER_ASSET "android.lua"

static int window_ready = 0;
static int gained_focus = 0;


static void handle_cmd(struct android_app* app, int32_t cmd) {
    switch (cmd) {
        case APP_CMD_INIT_WINDOW:
            // The window is being shown, get it ready.
            window_ready = 1;
            LOGV("%s: activity window ready.", TAG);
            break;
        case APP_CMD_GAINED_FOCUS:
            gained_focus = 1;
            LOGV("%s: activity gained focus.", TAG);
            break;
    }
}

void android_main(struct android_app* state) {
    lua_State *L;
    AAsset* luaCode;
    const void *buf;
    off_t bufsize;
    int status;

    LOGD("%s: starting", TAG);

    // wait until the activity is initialized before launching LuaJIT assets
    state->onAppCmd = handle_cmd;
    LOGV("%s: waiting for activity", TAG);
    int events;
    struct android_poll_source* source;
    // we block forever waiting for events.
    while (ALooper_pollAll(-1, NULL, &events, (void**)&source) >= 0) {
        // Process this event.
        if (source != NULL) {
            source->process(state, source);
        }
        if (window_ready && gained_focus) {
            break;
        }
        // Check if we are exiting.
        if (state->destroyRequested != 0) {
            return;
        }
    }

    LOGV("%s: launching LuaJIT assets", TAG);
    luaCode = AAssetManager_open(state->activity->assetManager, LOADER_ASSET, AASSET_MODE_BUFFER);
    if (luaCode == NULL) {
        LOGE("%s: error loading loader asset", TAG);
        goto quit;
    }

    bufsize = AAsset_getLength(luaCode);
    buf = AAsset_getBuffer(luaCode);
    if (buf == NULL) {
        LOGE("%s: error getting loader asset buffer", TAG);
        goto quit;
    }

    // Load initial Lua loader from our asset store:

    L = luaL_newstate();
    luaL_openlibs(L);

    status = luaL_loadbuffer(L, (const char*) buf, (size_t) bufsize, LOADER_ASSET);
    AAsset_close(luaCode);
    if (status) {
        LOGE("%s: error loading file: %s", TAG, lua_tostring(L, -1));
        goto quit;
    }

    // pass the android_app state to Lua land:
    lua_pushlightuserdata(L, state);

    status = lua_pcall(L, 1, LUA_MULTRET, 0);
    if (status) {
        LOGE("%s: failed to run script: %s", TAG, lua_tostring(L, -1));
        goto quit;
    }

    lua_close(L);

quit:
    LOGE("%s: Stopping due to previous errors", TAG);
    ANativeActivity_finish(state->activity);
    exit(1);
}
