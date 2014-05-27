local ffi = require("ffi")
local A = require("android")

--require("test")

require("libs/libkoreader-lfs")

-- the default current directory is root so we should first of all
-- change current directory to application's data directory
if lfs.chdir(A.dir) then
    A.LOGI("Change directory to "..lfs.currentdir())
else
    A.LOGE("Cannot change directory to "..A.dir)
end

Blitbuffer = require("ffi/blitbuffer")
freetype = require("ffi/freetype")
Image = require("ffi/mupdfimg")
util = require("ffi/util")
einkfb = require("ffi/framebuffer")
input = require("ffi/input_android")

ARGV = {"-d", "/sdcard"}
dofile(A.dir.."/reader.lua")

