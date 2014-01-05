/*
 * Copyright (C) 2014 The Koreader Project / Hans-Werner Hilse
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

#include <android_native_app_glue.h>
#include <android/log.h>

#include "luajit-2.0/lua.h"
#include "luajit-2.0/lauxlib.h"

#define  LOG_TAG    "luajit-trampoline"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

void android_main(struct android_app* state) {
	lua_State *L;
	int status;

	// Make sure glue isn't stripped.
	app_dummy();

	L = luaL_newstate();
	luaL_openlibs(L);

	status = luaL_loadfile(L, "/sdcard/koreader/main.lua");
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
