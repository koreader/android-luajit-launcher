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

ffi.cdef[[
/* from /usr/include/elf.h */

/* Type for a 16-bit quantity.  */
typedef uint16_t Elf32_Half;

/* Types for signed and unsigned 32-bit quantities.  */
typedef uint32_t Elf32_Word;
typedef int32_t  Elf32_Sword;

/* Types for signed and unsigned 64-bit quantities.  */
typedef uint64_t Elf32_Xword;
typedef int64_t  Elf32_Sxword;

/* Type of addresses.  */
typedef uint32_t Elf32_Addr;

/* Type of file offsets.  */
typedef uint32_t Elf32_Off;

/* Type for section indices, which are 16-bit quantities.  */
typedef uint16_t Elf32_Section;

/* Type for version symbol information.  */
typedef Elf32_Half Elf32_Versym;

typedef struct
{
  unsigned char e_ident[16];            /* Magic number and other info */
  Elf32_Half    e_type;                 /* Object file type */
  Elf32_Half    e_machine;              /* Architecture */
  Elf32_Word    e_version;              /* Object file version */
  Elf32_Addr    e_entry;                /* Entry point virtual address */
  Elf32_Off     e_phoff;                /* Program header table file offset */
  Elf32_Off     e_shoff;                /* Section header table file offset */
  Elf32_Word    e_flags;                /* Processor-specific flags */
  Elf32_Half    e_ehsize;               /* ELF header size in bytes */
  Elf32_Half    e_phentsize;            /* Program header table entry size */
  Elf32_Half    e_phnum;                /* Program header table entry count */
  Elf32_Half    e_shentsize;            /* Section header table entry size */
  Elf32_Half    e_shnum;                /* Section header table entry count */
  Elf32_Half    e_shstrndx;             /* Section header string table index */
} Elf32_Ehdr;

/* Section header.  */

typedef struct
{
  Elf32_Word    sh_name;                /* Section name (string tbl index) */
  Elf32_Word    sh_type;                /* Section type */
  Elf32_Word    sh_flags;               /* Section flags */
  Elf32_Addr    sh_addr;                /* Section virtual addr at execution */
  Elf32_Off     sh_offset;              /* Section file offset */
  Elf32_Word    sh_size;                /* Section size in bytes */
  Elf32_Word    sh_link;                /* Link to another section */
  Elf32_Word    sh_info;                /* Additional section information */
  Elf32_Word    sh_addralign;           /* Section alignment */
  Elf32_Word    sh_entsize;             /* Entry size if section holds table */
} Elf32_Shdr;

/* Dynamic section entry.  */

typedef struct
{
  Elf32_Sword   d_tag;                  /* Dynamic entry type */
  union
    {
      Elf32_Word d_val;                 /* Integer value */
      Elf32_Addr d_ptr;                 /* Address value */
    } d_un;
} Elf32_Dyn;

static const int SHT_STRTAB = 3;
static const int SHT_DYNAMIC = 6;

static const int DT_NEEDED = 1;
]]

local Elf = {__index={}}

-- open an ELF file
function Elf.open(filename)
    local e = {}
    e.filename = filename
    e.file = assert(io.open(filename, "r"), "cannot open file "..filename)
    -- should also raise error if 'filename' is a directory
    assert(e.file:read(0), filename .. " is not a regular file")
    setmetatable(e, Elf)
    return e
end

-- close file when object is garbage collected
function Elf:__gc()
    self.file:close()
end

-- convenience method that seeks and reads and also casts to an FFI ctype
function Elf.__index:read_at(pos, whence, ctype, size)
    local t
    if size then
        t = ffi.new(ctype, size)
    else
        t = ffi.new(ctype)
    end
    self.file:seek(whence, pos)
    s = assert(self.file:read(size or ffi.sizeof(t)),
        "cannot read from file "..self.filename)
    assert(#s == size or ffi.sizeof(t), "too short read from "..self.filename)
    ffi.copy(t, s, #s)
    return t
end

-- read the list of libraries that are needed by the ELF file
function Elf.__index:dlneeds()
    -- ELF header:
    local hdr = self:read_at(0, "set", "Elf32_Ehdr")

    -- Fetch string tables
    local shdr = self:read_at(
        hdr.e_shoff + hdr.e_shstrndx * ffi.sizeof("Elf32_Shdr"),
        "set", "Elf32_Shdr")
    local shstrtab = self:read_at(shdr.sh_offset, "set", "char[?]", shdr.sh_size)

    -- read .dynstr string table which contains the actual library names
    local dynstr
    self.file:seek("set", hdr.e_shoff)
    for i = 0, hdr.e_shnum - 1 do
        shdr = self:read_at(0, "cur", "Elf32_Shdr")
        if shdr.sh_type == ffi.C.SHT_STRTAB
        and ffi.string(shstrtab + shdr.sh_name) == ".dynstr" then
            dynstr = self:read_at(shdr.sh_offset, "set", "char[?]", shdr.sh_size)
            break
        end
    end
    assert(dynstr, "no .dynstr section in "..self.filename)

    local needs = {}
    -- walk through the table of needed libraries
    self.file:seek("set", hdr.e_shoff)
    for i = 0, hdr.e_shnum - 1 do
        shdr = self:read_at(0, "cur", "Elf32_Shdr")
        if shdr.sh_type == ffi.C.SHT_DYNAMIC then
            local offs = 0
            self.file:seek("set", shdr.sh_offset)
            while offs < shdr.sh_size do
                local dyn = self:read_at(0, "cur", "Elf32_Dyn")
                offs = offs + ffi.sizeof(dyn)
                if dyn.d_tag == ffi.C.DT_NEEDED then
                    table.insert(needs, ffi.string(dynstr + dyn.d_un.d_val))
                end
            end
            break
        end
    end

    return needs
end

return Elf
