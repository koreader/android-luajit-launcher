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

#ifdef KO_DLOPEN_LUAJIT
#  include <dlfcn.h>
#  include <sys/mman.h>
#endif

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

    // Shitty hack so that base can discriminate Android...
    setenv("IS_ANDROID", "true", 1);

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

#ifdef KO_DLOPEN_LUAJIT
    // Crappy workaround for mcode allocation issues
    // c.f., android.lua for more details.
    // The idea is to push the libluajit.so mapping "far" enough away,
    // that LuaJIT then succeeds in mapping mcode area(s) +/- 32MB (on arm, 128 MB on aarch64, 2GB on x86)
    // from lj_vm_exit_handler (c.f., mcode_alloc @ lj_mcode.c)
    // ~128MB works out rather well on the API levels where this actually achieves something (while it doesn't even faze some).
    const size_t map_size = 144U * 1024U * 1024U;
    void* p = mmap(NULL, map_size, PROT_NONE, MAP_PRIVATE | MAP_ANONYMOUS | MAP_NORESERVE, -1, 0);
    if (p == MAP_FAILED) {
        LOGE("%s: error allocating mmap for mcode alloc workaround", TAG);
        goto quit;
    }

    // Resolve everything *now*, and put the symbols in the global scope, much like if we had linked it statically.
    // This is necessary in order to be able to require Lua/C modules, c.f., LuaJIT docs on embedding.
    // (Beware, Android's dynamic linker has a long history of weird and broken behavior,
    // c.f., https://android.googlesource.com/platform/bionic/+/refs/heads/master/android-changes-for-ndk-developers.md)
    void* luajit = dlopen("libluajit.so", RTLD_NOW | RTLD_GLOBAL);
    if (!luajit) {
        LOGE("%s: failed to load LuaJIT: %s", TAG, dlerror());
    } else {
        dlerror();
    }

    // And free the mmap, its sole purpose is to push libluajit.so away in the virtual memory mappings.
    munmap(p, map_size);

    // Get all the symbols we'll need now
    lua_State* (*lj_luaL_newstate)(void) = dlsym(luajit, "luaL_newstate");
    void (*lj_luaL_openlibs)(lua_State*) = dlsym(luajit, "luaL_openlibs");
    int (*lj_luaL_loadbuffer)(lua_State*, const char*, size_t,  const char*) = dlsym(luajit, "luaL_loadbuffer");
    const char* (*lj_lua_tolstring)(lua_State *, int, size_t *) = dlsym(luajit, "lua_tolstring");
    void (*lj_lua_pushlightuserdata)(lua_State*, void*) = dlsym(luajit, "lua_pushlightuserdata");
    int (*lj_lua_pcall)(lua_State *, int, int, int) = dlsym(luajit, "lua_pcall");
    void (*lj_lua_close)(lua_State*) = dlsym(luajit, "lua_close");

    // Recap where things end up for our mcode_alloc shenanigans...
    LOGV("%s: mmap for mcode alloc workaround mmap was @ %p to %p", TAG, p, p + map_size);
    uintptr_t lj_mcarea_target = (uintptr_t) lj_lua_pcall & ~(uintptr_t) 0xffff;
    LOGV("%s: LuaJIT is mapped around %p", TAG, (void *) lj_mcarea_target);
    void* g_lj_mcarea_reserve = dlsym(luajit, "g_lj_mcarea_reserve");
    LOGV("%s: LuaJIT reserved mcarea is @ %p", TAG, g_lj_mcarea_reserve);

    // Load initial Lua loader from our asset store:
    L = (*lj_luaL_newstate)();
    (*lj_luaL_openlibs)(L);

    status = (*lj_luaL_loadbuffer)(L, (const char*) buf, (size_t) bufsize, LOADER_ASSET);
    AAsset_close(luaCode);
    if (status) {
        LOGE("%s: error loading file: %s", TAG, (*lj_lua_tolstring)(L, -1, NULL));
        goto quit;
    }

    // pass the android_app state to Lua land:
    (*lj_lua_pushlightuserdata)(L, state);

    status = (*lj_lua_pcall)(L, 1, LUA_MULTRET, 0);
    if (status) {
        LOGE("%s: failed to run script: %s", TAG, (*lj_lua_tolstring)(L, -1, NULL));
        goto quit;
    }

    (*lj_lua_close)(L);
#else
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
#endif

quit:
    LOGE("%s: Stopping due to previous errors", TAG);
    ANativeActivity_finish(state->activity);
    exit(1);
}
