local ffi = require("ffi")

ffi.cdef[[
    int poll(struct pollfd *fds, unsigned long nfds, int timeout);
]]

local utils = {}

function utils.sleep(seconds)
    if type(seconds) == "number" then
        ffi.C.poll(nil, 0, seconds * 1000)
    end
end

return utils
