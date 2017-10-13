local ffi = require("ffi")
local A = require("android")

ffi.cdef[[
int chdir(const char *path);
char *getcwd(char *buf, size_t size);
char *strerror(int errnum);
]]

local function chdir(path)
    if ffi.C.chdir(path) == 0 then
        return true
    else
        err_msg = "Unable to change working directory to '" ..path.."'"
        return nil, ffi.string(ffi.C.strerror(ffi.errno()))
    end
end

local max_path = 4096
local function currentdir()
    return ffi.string(
        ffi.C.getcwd(ffi.new('char[?]', max_path), max_path))
end

--require("test")

-- the default current directory is root so we should first of all
-- change current directory to application's data directory
if chdir(A.dir) then
    A.LOGI("Change directory to "..currentdir())
else
    A.LOGE("Cannot change directory to "..A.dir)
end

local function launch()
    dofile(A.dir.."/llapp_main.lua")
end

launch()
