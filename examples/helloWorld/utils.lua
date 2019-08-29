local ffi = require("ffi")

ffi.cdef[[
    unsigned int sleep(unsigned int seconds);
]]

local utils = {}

function utils.sleep(seconds)
    if type(seconds) == "number" then
        ffi.C.sleep(ffi.new("unsigned int", seconds))
    end
end

return utils
