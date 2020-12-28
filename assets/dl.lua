--[[
A LuaJIT FFI based version of dlopen() which loads dependencies
first (for implementations of dlopen() lacking that feature, like
on Android before API 23,
c.f., https://android.googlesource.com/platform/bionic/+/refs/heads/master/android-changes-for-ndk-developers.md)

This is heavily inspired by the lo_dlopen() implementation from
LibreOffice (see
https://cgit.freedesktop.org/libreoffice/core/tree/sal/android/lo-bootstrap.c?id=963c98a65e4eddf179e170ff0bb30e4bfafc6b16)
and as such:

 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
--]]

-- Disable the JIT in this module, to avoid weird and mysterious issues with dlopen (here and in ffi.load),
-- as well as the nested loops in dl.dlopen mysteriously breaking early.
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

local function sys_dlopen(library, global, padding)
    A.LOGVV(log, string.format("%"..padding.."ssys_dlopen - loading library %s (in %s namespace)", "", library, global and "global" or "local"))
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
function dl.dlopen(library, load_func, depth)
    load_func = load_func or sys_dlopen
    depth = depth or 0
    local padding = depth * 4

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
            A.LOGVV(log, string.format("%"..padding.."sdl.dlopen - %s => %s", "", library, lname))
            depth = depth + 1
            padding = depth * 4
            -- we found a library, now load its requirements
            -- we do _not_ pass the load_func to the cascaded
            -- calls, so those will always use sys_dlopen()
            local lib_needs = lib:dlneeds()
            lib:close()
            for i, needed in ipairs(lib_needs) do
                -- That's the pspec of the original *library*, and not of this *needed*
                -- (i.e., we skip loading transitive dependencies of system libraries
                -- (whether those are system libs or not),
                -- but not system libraries themselves when they're depedencies of bundled libs,
                -- and not when they're the actual library we requested to load).
                if pspec ~= "/system/lib" then
                    -- For Android >= 6.0, the list of safe system libraries is:
                    -- libandroid, libc, libcamera2ndk, libdl, libGLES, libjnigraphics,
                    -- liblog, libm, libmediandk, libOpenMAXAL, libOpenSLES, libstdc++,
                    -- libvulkan, and libz
                    A.LOGVV(log, string.format("%"..padding.."sdl.dlopen - needed => %s (%d of %d) <= %s", "", needed, i, #lib_needs, lname))
                    dl.dlopen(needed, sys_dlopen, depth)
                end
            end
            depth = depth - 1
            padding = depth * 4
            if load_func == sys_dlopen then
                return sys_dlopen(lname, false, padding)
            else
                A.LOGVV(log, string.format("%"..padding.."sdl.dlopen - load_func -> %s", "", lname))
                return load_func(lname)
            end
        end
    end

    error("could not find library " .. library)
end

return dl
