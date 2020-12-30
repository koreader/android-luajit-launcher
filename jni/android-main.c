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
#include <dlfcn.h>
#include <sys/mman.h>

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

static void *mmap_at(uintptr_t hint, size_t sz)
{
    void *p = mmap((void *)hint, sz, PROT_NONE, MAP_PRIVATE | MAP_ANONYMOUS | MAP_NORESERVE, -1, 0);
    if (p == MAP_FAILED) {
        p = NULL;
    }
    LOGV("%s: got an mmap @ %p", TAG, p);
    return p;
}


static void mmap_free(void *p, size_t sz)
{
    LOGV("%s: unmapped @ %p", TAG, p);
    munmap(p, sz);
}

#define ALIGN_UP(x, a)                                                                                       \
({                                                                                                           \
    __auto_type mask__ = (a) -1U;                                                                            \
    (((x) + (mask__)) & ~(mask__));                                                                          \
})

#define ALIGN_DOWN(x, a)                                                                                     \
({                                                                                                           \
    __auto_type mask__ = (a) -1U;                                                                            \
    ((x) & ~(mask__));                                                                                       \
})

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

    // Shitty workaround for mcode allocation issues
    // c.f., android.lua for more details.
    // The idea is to push the libluajit.so mapping "far" enough away,
    // that LuaJIT then succeeds in mapping mcode area(s) +/- 32MB (on arm, 128 MB on aarch64, 2GB on x86)
    // from lj_vm_exit_handler (c.f., mcode_alloc @ lj_mcode.c)
    // ~128MB works out rather well (we're near the top of the allocs, soon after the last [dalvik-non moving space])

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

    // Recap where things end up...
    LOGV("%s: mmap for mcode alloc workaround was @ %p to %p", TAG, p, p + map_size);
    uintptr_t lj_mcarea_target = (uintptr_t) lj_lua_pcall & ~(uintptr_t) 0xffff;
    LOGV("%s: LuaJIT is mapped around %p", TAG, (void *) lj_mcarea_target);
    uintptr_t g_lj_mcarea_reserve = dlsym(luajit, "g_lj_mcarea_reserve");
    LOGV("%s: LuaJIT reserve mcarea is @ %p", TAG, (void *) g_lj_mcarea_reserve);

    // NOTE: On some devices, reserving larger areas has a tendency to punt the mapping off to wherever (generally too far),
    //       (and then repeatedly get the same address or around that first one),
    //       so, keep it small. We mostly want a 512K block anyway ;).
    // Reserve a nearly 1MB area near that (based on LuaJIT's lj_mcode.c and the arm jumprange: +-2^25 = +-32MB)
    void * reserve_start = NULL;
    const size_t jumprange = 25U;
    const size_t reserve_size = 0xf0000u;

    // Half the jumprange minus some 2MB of change
    const uintptr_t range = (1U << (jumprange - 1U)) - (1U << 21U);
    uintptr_t hint = lj_mcarea_target;
    uintptr_t shift = 0U;
    // Limit probing iterations, depending on the jump range and the iteration shift
    for (size_t i = 0U; i < ((1U << jumprange) / 0x10000u); i++) {
        LOGV("%s: iter %d of %u", TAG, i, ((1U << jumprange) / 0x10000u));
        uintptr_t mapping = hint;
        if (hint != NULL) {
            LOGV("%s: requesting mmap @ %p", TAG, (void *) hint);
            void *p = mmap_at(hint, reserve_size);
            mapping = (uintptr_t) p;

            if (p != NULL &&
               ((uintptr_t)p + reserve_size - lj_mcarea_target < range || lj_mcarea_target - (uintptr_t)p < range)) {
                // Got it!
                reserve_start = p;
                LOGV("%s: Match @ %p", TAG, p);
                break;
            }
            if (p) {
                // Free badly placed area.
                LOGV("%s: OOR @ %p", TAG, p);
                mmap_free(p, reserve_size);
            }
        } else {
            LOGV("%s: invalid hint @ %p", TAG, (void *) hint);
        }
        // Next, try probing 64K away (alternate up and down)...
        shift += 0x10000u;
        LOGV("%s: shift hint by %p", TAG, (void *) shift);
        if ((i & 0x01u) == 0u) {
            hint = ALIGN_UP(lj_mcarea_target + shift, 0x10000u);
        } else {
            hint = ALIGN_DOWN(lj_mcarea_target - shift, 0x10000u);
        }
        LOGV("%s: next hint @ %p", TAG, (void *) hint);
    }

    // We failed to reserve a suitable mcode region...
    if (reserve_start == NULL) {
        LOGE("%s: failed to reserve an mcode region", TAG);
        goto quit;
    } else {
        // FIXME: Compute effective range
        LOGV("%s: Reserved an mcode area for LuaJIT @ %p", TAG, (void *) reserve_start);
        char reserve_str[64] = { 0 };
        snprintf(reserve_str, sizeof(reserve_str) - 1U, "%p", (void *) reserve_start);
        setenv("LUAJIT_MCAREA_START", reserve_str, 1);
    }

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

quit:
    LOGE("%s: Stopping due to previous errors", TAG);
    ANativeActivity_finish(state->activity);
    exit(1);
}
