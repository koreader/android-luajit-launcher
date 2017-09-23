--[[
A LuaJIT FFI based version of dlopen() which loads dependencies
first (for implementations of dlopen() lacking that feature, like
on Android)

This is heavily inspired by the lo_dlopen() implementation from
LibreOffice (see
http://cgit.freedesktop.org/libreoffice/core/tree/sal/android/lo-bootstrap.c)
and as such:

 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
--]]

local ffi = require("ffi")
local Elf = require("elf")

ffi.cdef[[
void *dlopen(const char *filename, int flag);
char *dlerror(void);
const static int RTLD_LOCAL = 0;
const static int RTLD_GLOBAL = 0x00100;
]]

local dl = {
    -- set this to search in certain directories
    library_path = '/lib/?;/usr/lib/?;/usr/local/lib/?',

    loaded_libraries = {}
}

local function sys_dlopen(library)
    local p = ffi.C.dlopen(library, ffi.C.RTLD_LOCAL)
    if p == nil then
        local err_msg = ffi.C.dlerror()
        if err_msg ~= nil then
            error("error opening "..library..": "..ffi.string(err_msg))
        end
    end
    return p
end

--[[
open_func will be used to load the library (but not its dependencies!)
if not given, the system's dlopen() will be used

if the library name is an absolute path (starting with "/"), then
the library_path will not be used
--]]
function dl.dlopen(library, load_func)
    -- check if we already opened it:
    if dl.loaded_libraries[library] then return loaded_libraries[library] end

    load_func = load_func or sys_dlopen

    for pspec in string.gmatch(
            library:sub(1,1) == "/" and "" or dl.library_path,
            "([^;:]+)") do

        local lname, matches = string.gsub(pspec, "%?", library)
        if matches == 0 then
            -- if pathspec does not contain a '?', we do append
            -- the library name to the pathspec
            lname = lname .. '/' .. library
        end

        local ok, lib = pcall(Elf.open, lname)
        if not ok and lname:find("%.so%.%d+$") then
            lname = lname:gsub("%.so%.%d+$", "%.so")
            ok, lib = pcall(Elf.open, lname)
        end
        if ok then
            -- we found a library, now load its requirements
            -- we do _not_ pass the load_func to the cascaded
            -- calls, so those will always use sys_dlopen()
            for _, needed in pairs(lib:dlneeds()) do
                if needed == "libluajit.so" then
                    -- load the luajit-launcher libluajit with sys_dlopen
                    load_func("libluajit.so")
                elseif needed ~= "libdl.so" then
                    -- for android >= 6.0, you can't load system library anymore
                    -- and since we also have our own dl implementation, it's safe
                    -- to skip the stock libdl.
                    dl.dlopen(needed)
                end
            end
            return load_func(lname)
        end
    end

    error("could not find library " .. library)
end

return dl
