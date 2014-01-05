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

#include <android_native_app_glue.h>

#include <android/log.h>
#include <android/asset_manager.h>

#include "luajit-2.0/lua.h"
#include "luajit-2.0/lauxlib.h"

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"luajit-launcher",__VA_ARGS__)
#define  LOADER_ASSET "loader.lua"

void android_main(struct android_app* state) {
	lua_State *L;
	AAsset* luaCode;
	const void *buf;
	off_t bufsize;
	int status;

	// Make sure glue isn't stripped.
	app_dummy();

	luaCode = AAssetManager_open(state->activity->assetManager, LOADER_ASSET, AASSET_MODE_BUFFER);
	if (luaCode == NULL) {
		LOGE("error loading loader asset");
		return;
	}

	bufsize = AAsset_getLength(luaCode);
	buf = AAsset_getBuffer(luaCode);
	if (buf == NULL) {
		LOGE("error getting loader asset buffer");
		return;
	}

	// Load initial Lua loader from our asset store:

	L = luaL_newstate();
	luaL_openlibs(L);

	status = luaL_loadbuffer(L, (const char*) buf, (size_t) bufsize, LOADER_ASSET);
	AAsset_close(luaCode);
	if (status) {
		LOGE("error loading file: %s", lua_tostring(L, -1));
		return;
	}

	// pass the android_app state to Lua land:
	lua_pushlightuserdata(L, state);
	lua_setglobal(L, "android_app_state");

	status = lua_pcall(L, 0, LUA_MULTRET, 0);
	if (status) {
		LOGE("Failed to run script: %s", lua_tostring(L, -1));
		return;
	}

	lua_close(L);
}
