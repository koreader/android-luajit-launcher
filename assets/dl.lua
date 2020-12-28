--[[
A LuaJIT FFI based version of dlopen() which loads dependencies
first (for implementations of dlopen() lacking that feature, like
on Android before API 23,
c.f., https://android.googlesource.com/platform/bionic/+/refs/heads/master/android-changes-for-ndk-developers.md)

This is heavily inspired by the lo_dlopen() implementation from
LibreOffice (see
http://cgit.freedesktop.org/libreoffice/core/tree/sal/android/lo-bootstrap.c)
and as such:

 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
--]]

-- Disable the JIT in this module, to avoid weird and mysterious issues with dlopen (here and in ffi.load)
jit.off(true, true)

local ffi = require("ffi")
local A = require("android")
local Elf = require("elf")
local log = "dlopen"

local C = ffi.C

-- There's a bit of heinous hackery going on on 32-bit ABIs with RTLD_NOW & RTLD_GLOBAL...
-- c.f., https://android.googlesource.com/platform/bionic/+/refs/heads/master/libc/include/dlfcn.h
if jit.arch:sub(-2) == "64" then
    ffi.cdef[[
    void *dlopen(const char *filename, int flag);
    char *dlerror(void);
    const static int RTLD_LOCAL    = 0;
    const static int RTLD_LAZY     = 0x00001;
    const static int RTLD_NOW      = 0x00002;
    const static int RTLD_NOLOAD   = 0x00004;
    const static int RTLD_GLOBAL   = 0x00100;
    const static int RTLD_NODELETE = 0x01000;
    ]]
else
    ffi.cdef[[
    void *dlopen(const char *filename, int flag);
    char *dlerror(void);
    const static int RTLD_LOCAL    = 0;
    const static int RTLD_LAZY     = 0x00001;
    const static int RTLD_NOW      = 0x00000;
    const static int RTLD_NOLOAD   = 0x00004;
    const static int RTLD_GLOBAL   = 0x00002;
    const static int RTLD_NODELETE = 0x01000;
    ]]
end

local dl = {
    -- set this to search in certain directories
    library_path = '/lib/?;/usr/lib/?;/usr/local/lib/?',
}

local function sys_dlopen(library, global)
    A.LOGVV(log, string.format("sys_dlopen - loading library %s (in %s namespace)", library, global and "global" or "local"))
    local p = C.dlopen(library, bit.bor(C.RTLD_NOW, global and C.RTLD_GLOBAL or C.RTLD_LOCAL))
    if p == nil then
        local err_msg = C.dlerror()
        if err_msg ~= nil then
            error("error opening "..library..": "..ffi.string(err_msg))
        end
    else
        C.dlerror()
    end
    return p
end

--[[
load_func will be used to load the library (but not its dependencies!)
if not given, the system's dlopen() will be used

if the library name is an absolute path (starting with "/"), then
the library_path will not be used
--]]
function dl.dlopen(library, load_func)
    load_func = load_func or sys_dlopen

    for pspec in string.gmatch(
            library:sub(1, 1) == "/" and "" or dl.library_path,
            "([^;:]+)") do

        local lname, matches = string.gsub(pspec, "%?", library)
        if matches == 0 then
            -- if pathspec does not contain a '?',
            -- we append the library name to the pathspec
            lname = lname .. '/' .. library
        end

        local ok, lib = pcall(Elf.open, lname)
        if not ok and lname:find("%.so%.%d+$") then
            lname = lname:gsub("%.so%.%d+$", "%.so")
            ok, lib = pcall(Elf.open, lname)
        end
        if ok then
            A.LOGVV(log, string.format("dl.dlopen - library lname detected %s", lname))

            -- we found a library, now load its requirements
            -- we do _not_ pass the load_func to the cascaded
            -- calls, so those will always use sys_dlopen()
            local lib_needs = lib:dlneeds()
            lib:close()
            for i, needed in ipairs(lib_needs) do
                if needed == "libluajit.so" then
                    -- Load our libluajit with sys_dlopen
                    -- This should be mostly unnecessary, except possibly on very old Android versions with an extremely broken linker/loader,
                    -- as we already dlopen luajit w/ RTLD_GLOBAL in the launcher...
                    sys_dlopen("libluajit.so", false)
                elseif pspec ~= "/system/lib" then
                    -- For Android >= 6.0, the list of safe system libraries is:
                    -- libandroid, libc, libcamera2ndk, libdl, libGLES, libjnigraphics,
                    -- liblog, libm, libmediandk, libOpenMAXAL, libOpenSLES, libstdc++,
                    -- libvulkan, and libz
                    A.LOGVV(log, string.format("         dl.dlopen - opening needed %s (%d of %d) for %s", needed, i, #lib_needs, lname))
                    dl.dlopen(needed, sys_dlopen)
                    A.LOGVV(log, string.format("         dl.dlopen - Back from a recursive call on needed %s (%d of %d) for %s", needed, i, #lib_needs, lname))
                end
            end
            if load_func == sys_dlopen then
                A.LOGVV(log, string.format("dl.dlopen - deferring loading of %s to sys_dlopen", lname))
            else
                A.LOGVV(log, string.format("dl.dlopen - deferring loading of %s to load_func", lname))
            end
            return load_func(lname)
        end
    end

    error("could not find library " .. library)
end

return dl
