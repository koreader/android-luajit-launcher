local ffi = require("ffi")

ffi.cdef[[
static const int EI_CLASS = 4;
static const int ELFCLASSNONE = 0;
static const int ELFCLASS32 = 1;
static const int ELFCLASS64 = 2;
static const int SHT_STRTAB = 3;
static const int SHT_DYNAMIC = 6;
static const int DT_NEEDED = 1;
typedef short unsigned int Elf32_Half;
typedef unsigned int Elf32_Word;
typedef int Elf32_Sword;
typedef long long unsigned int Elf32_Xword;
typedef long long int Elf32_Sxword;
typedef unsigned int Elf32_Addr;
typedef unsigned int Elf32_Off;
typedef short unsigned int Elf32_Section;
typedef short unsigned int Elf32_Versym;
typedef struct {
  unsigned char e_ident[16];
  Elf32_Half e_type;
  Elf32_Half e_machine;
  Elf32_Word e_version;
  Elf32_Addr e_entry;
  Elf32_Off e_phoff;
  Elf32_Off e_shoff;
  Elf32_Word e_flags;
  Elf32_Half e_ehsize;
  Elf32_Half e_phentsize;
  Elf32_Half e_phnum;
  Elf32_Half e_shentsize;
  Elf32_Half e_shnum;
  Elf32_Half e_shstrndx;
} Elf32_Ehdr;
typedef struct {
  Elf32_Word sh_name;
  Elf32_Word sh_type;
  Elf32_Word sh_flags;
  Elf32_Addr sh_addr;
  Elf32_Off sh_offset;
  Elf32_Word sh_size;
  Elf32_Word sh_link;
  Elf32_Word sh_info;
  Elf32_Word sh_addralign;
  Elf32_Word sh_entsize;
} Elf32_Shdr;
typedef struct {
  Elf32_Sword d_tag;
  union {
    Elf32_Word d_val;
    Elf32_Addr d_ptr;
  } d_un;
} Elf32_Dyn;
]]
