android-luajit-launcher
=======================

Android NativeActivity based launcher for LuaJIT, implementing the main loop within Lua land via FFI.

NativeActivity is available starting with platform android-9.

Have a look at the Android NDK's "native-plasma" sample activity to get an idea what it does - or rather, is theoretically able to do. While the sample from NDK implements everything in C, in our case, we create a LuaJIT instance and hand off control to it instead. LuaJIT then handles the main loop. In this programming model, we have a thread which presents us with a "main" entry point and allow us to follow our own program flow as long as we poll for and react to events.

A good number of Android native API headers are readily presented via FFI already. I'll probably add more along the way.

For now - and probably ever, since Mike Pall recommends strongly to do so - the compilation of LuaJIT is not integrated into the Android build framework and has to be run separately.

A wrapper script for building LuaJIT is provided.

Have a look at KOReader's [llapp_main.lua](https://github.com/koreader/koreader/blob/master/platform/android/llapp_main.lua) file. You can use it as a starting point for your own app.

The real starting point, called from JNI/C, is the run() function in android.lua. It sets up a few things, namely FFI definitions for the Android native API (since it uses that itself for a few things) and some wrapper functions for logging. Also, it registers the "android" module in package.loaded, so you can access it in your own code via require("android"). It also registers a new package loader which can load Lua code from the activity's asset store, so you can use require() for Lua code stored there.


STARTING:
========
* init/update the submodules (LuaJIT for now):
```
git submodule init
git submodule sync
git submodule update
```

* update the project for your Android SDK/NDK (needs "android" tool in your PATH):
```
android update project --path .
```

* compile LuaJIT for your target architecture(s):
```
./mk-luajit.sh armeabi-v7a
./mk-luajit.sh clean
./mk-luajit.sh armeabi
```

* compile NDK/native code:
```
ndk-build
```

* compile and package APK (debug variant):
```
ant debug
```

TODO:
========

* a concept to deal with native Lua modules.
* a loader for native modules that have been put into the activity's library directory?
* a loader for obb storage, maybe? We could put native Lua modules there, for example.
* example code for framebuffer access and more
