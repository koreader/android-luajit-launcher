--[[
Tools for handling ELF files

not much implemented for now - just enough to parse 32bit
ELF files for their needed libraries.

This is heavily inspired by the lo_dlopen() implementation from
LibreOffice (see
http://cgit.freedesktop.org/libreoffice/core/tree/sal/android/lo-bootstrap.c)
and as such:

 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
--]]

local ffi = require("ffi")

local C = ffi.C

-- Pull in the necessary cdefs
require("elf_h")

local Elf = {__index={}}

-- open an ELF file
function Elf.open(filename)
    local e = {}
    e.filename = filename
    -- Slightly more roundabout than e.file = assert(io.open(filename, "r")) in order to preserve the errno...
    local err = {}
    e.file, err.str, err.num = io.open(filename, "r")
    assert(e.file, err)
    -- should also raise error if 'filename' is a directory
    assert(e.file:read(0))
    setmetatable(e, Elf)
    -- Check the Elf class (head of the Ehdr, which is at the head of the file)
    local e_ident = e:read_at(0, "set", "unsigned char[?]", C.EI_NIDENT)
    -- Check the ELF magic, first
    assert(e_ident[C.EI_MAG0] == C.ELFMAG0 and
           e_ident[C.EI_MAG1] == C.ELFMAG1 and
           e_ident[C.EI_MAG2] == C.ELFMAG2 and
           e_ident[C.EI_MAG3] == C.ELFMAG3,
           "not a valid ELF binary")
    -- Then the class
    e.class = e_ident[C.EI_CLASS]
    assert(e.class == C.ELFCLASS32 or e.class == C.ELFCLASS64, "invalid ELF class")
    -- Set the ctypes we'll use given the Elf class
    if e.class == C.ELFCLASS64 then
        e.Elf_Ehdr = ffi.typeof("Elf64_Ehdr")
        e.Elf_Shdr = ffi.typeof("Elf64_Shdr")
        e.Elf_Dyn = ffi.typeof("Elf64_Dyn")
    else
        e.Elf_Ehdr = ffi.typeof("Elf32_Ehdr")
        e.Elf_Shdr = ffi.typeof("Elf32_Shdr")
        e.Elf_Dyn = ffi.typeof("Elf32_Dyn")
    end
    return e
end

-- close file when object is garbage collected
function Elf:__gc()
    if self.file ~= nil then
        self.file:close()
    end
end

function Elf.__index:close()
    if self.file ~= nil then
        self.file:close()
        self.file = nil
    end
end

-- convenience method that seeks and reads and also casts to an FFI ctype
function Elf.__index:read_at(pos, whence, ctype, size)
    -- We'll get a cdata instead of a plain Lua number with Elf64, coerce that back in a way seek handles
    pos = tonumber(pos)
    local t
    if size then
        t = ffi.new(ctype, size)
        -- Same idea as for pos above, io.read doesn't like a cdata size ;)
        size = tonumber(size)
    else
        t = ffi.new(ctype)
    end
    self.file:seek(whence, pos)
    local s = assert(self.file:read(size or ffi.sizeof(t)))
    assert(#s == size or ffi.sizeof(t), "short read")
    ffi.copy(t, s, #s)
    return t
end

-- read the list of libraries that are needed by the ELF file
function Elf.__index:dlneeds()
    -- ELF header:
    local hdr = self:read_at(0, "set", self.Elf_Ehdr)

    -- Fetch string tables
    local shdr_pos = tonumber(hdr.e_shoff + hdr.e_shstrndx * ffi.sizeof(self.Elf_Shdr))
    local shdr = self:read_at(shdr_pos, "set", self.Elf_Shdr)
    local shstrtab = self:read_at(shdr.sh_offset, "set", "char[?]", shdr.sh_size)

    -- read .dynstr string table which contains the actual library names
    local dynstr
    self.file:seek("set", tonumber(hdr.e_shoff))
    for i = 0, hdr.e_shnum - 1 do
        shdr = self:read_at(0, "cur", self.Elf_Shdr)
        if shdr.sh_type == C.SHT_STRTAB
        and ffi.string(shstrtab + shdr.sh_name) == ".dynstr" then
            dynstr = self:read_at(shdr.sh_offset, "set", "char[?]", shdr.sh_size)
            break
        end
    end
    assert(dynstr, "no .dynstr section")

    local needs = {}
    -- walk through the table of needed libraries
    self.file:seek("set", tonumber(hdr.e_shoff))
    for i = 0, hdr.e_shnum - 1 do
        shdr = self:read_at(0, "cur", self.Elf_Shdr)
        if shdr.sh_type == C.SHT_DYNAMIC then
            local offs = 0
            self.file:seek("set", tonumber(shdr.sh_offset))
            while offs < shdr.sh_size do
                local dyn = self:read_at(0, "cur", self.Elf_Dyn)
                offs = offs + ffi.sizeof(dyn)
                if dyn.d_tag == C.DT_NEEDED then
                    table.insert(needs, ffi.string(dynstr + dyn.d_un.d_val))
                end
            end
            break
        end
    end

    return needs
end

return Elf
