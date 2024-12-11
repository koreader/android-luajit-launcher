# android-luajit-launcher

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/193dcd3a4fe14bb48960a6473156c814)](https://app.codacy.com/gh/koreader/android-luajit-launcher/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

Android NativeActivity based launcher for LuaJIT, implementing the main loop within Lua land via FFI.

NativeActivity is available starting with platform android-9.

Have a look at the Android NDK's "native-plasma" sample activity to get an idea what it does - or rather, is theoretically able to do. While the sample from NDK implements everything in C, in our case, we create a LuaJIT instance and hand off control to it instead. LuaJIT then handles the main loop. In this programming model, we have a thread which presents us with a "main" entry point and allow us to follow our own program flow as long as we poll for and react to events.

A good number of Android native API headers are readily presented via FFI already. I'll probably add more along the way.

For now - and probably ever, since Mike Pall recommends strongly to do so - the compilation of LuaJIT is not integrated into the Android build framework and has to be run separately.

A wrapper script for building LuaJIT is provided. It **relies on NDK r15c**

Have a look at KOReader's [llapp_main.lua](https://github.com/koreader/koreader/blob/master/platform/android/llapp_main.lua) file. You can use it as a starting point for your own app.

The real starting point, called from JNI/C, is the run() function in android.lua. It sets up a few things, namely FFI definitions for the Android native API (since it uses that itself for a few things) and some wrapper functions for logging. Also, it registers the "android" module in package.loaded, so you can access it in your own code via require("android"). It also registers a new package loader which can load Lua code from the activity's asset store, so you can use require() for Lua code stored there.

## Disclaimer

This repo is used as glue code to run KOReader on Android.

Usage on its own doesn't make much sense since you're expected to write your own drawing routines in Lua!
A standalone example is attached (with no drawing routines!) in case you're curious what you'll get.


## Building the example app

### Export `ANDROID_NDK_HOME`

```sh
export ANDROID_NDK_HOME=/path/to/your/ndk/folder
```

### Build example APK for arm 32 bits

```sh
make example
```


### Build example APK for any other arch

```sh
ANDROID_ARCH=MY_ARCH make example
```

where `MY_ARCH` is either `x86`, `x86_64` or `arm64`
