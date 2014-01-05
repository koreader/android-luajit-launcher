android-luajit-launcher
=======================

Android NativeActivity based launcher for LuaJIT, implementing the main loop within Lua land via FFI.

Have a look at the Android NDK's "native-plasma" sample activity to get an idea what it does - or rather, is theoretically able to do. While the sample from NDK implements everything in C, in our case, we create a LuaJIT instance and hand off control to it instead. LuaJIT then handles the main loop. In this programming model, we have a thread which presents us with a "main" entry point and allow us to follow our own program flow as long as we poll for and react to events.

A good number of Android native API headers are readily presented via FFI already. I'll probably add more along the way.

For now - and probably ever, since Mike Pall recommends strongly to do so - the compilation of LuaJIT is not integrated into the Android build framework and has to be run separately.

A wrapper script for building LuaJIT is provided.
