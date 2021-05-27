local ffi = require("ffi")
local A = require("android")

local C = ffi.C

ffi.cdef[[
int chdir(const char *path);
]]

-- the default current directory is root so we should first of all
-- change current directory to application's data directory
if C.chdir(A.dir) ~= 0 then
    local err = "Unable to change working directory to '" .. A.dir .. "'"
    A.LOGE(err)
    error(err)
end

local function launch()
    dofile(A.dir.."/llapp_main.lua")
end

launch()
