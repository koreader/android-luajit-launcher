-- Simple wrapper around elf.lua to test its behavior.

-- On the off-chance it's as jittery as on Android...
jit.off(true, true)

local ffi = require("ffi")

package.path = "?.lua;" .. "utils/?.lua;" .. "assets/?.lua;" .. "../assets/?.lua;" .. package.path
local Elf = require("elf")

lname = arg[1]

local lib = Elf.open(lname)
local lib_needs = lib:dlneeds()
lib:close()
for i, needed in ipairs(lib_needs) do
    print(string.format("needed => %s (%d of %d) <= %s", needed, i, #lib_needs, lname))
end
