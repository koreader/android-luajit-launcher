#include <elf.h>

#include "ffi-cdecl.h"

// Constants
cdecl_const(EI_NIDENT)

cdecl_const(EI_MAG0)
cdecl_const(ELFMAG0)
cdecl_const(EI_MAG1)
cdecl_const(ELFMAG1)
cdecl_const(EI_MAG2)
cdecl_const(ELFMAG2)
cdecl_const(EI_MAG3)
cdecl_const(ELFMAG3)

cdecl_const(EI_CLASS)
cdecl_const(ELFCLASSNONE)
cdecl_const(ELFCLASS32)
cdecl_const(ELFCLASS64)


cdecl_const(SHT_STRTAB)
cdecl_const(SHT_DYNAMIC)

cdecl_const(DT_NEEDED)

// ELFCLASS32
cdecl_c99_type(Elf32_Half, uint16_t)
cdecl_c99_type(Elf32_Word, uint32_t)
cdecl_c99_type(Elf32_Sword, int32_t)

cdecl_c99_type(Elf32_Xword, uint64_t)
cdecl_c99_type(Elf32_Sxword, int64_t)

cdecl_c99_type(Elf32_Addr, uint32_t)

cdecl_c99_type(Elf32_Off, uint32_t)

cdecl_c99_type(Elf32_Section, uint16_t)

cdecl_c99_type(Elf32_Versym, Elf32_Half)

cdecl_type(Elf32_Ehdr)
cdecl_type(Elf32_Shdr)
cdecl_type(Elf32_Dyn)

// ELFCLASS64
cdecl_c99_type(Elf64_Half, uint16_t)
cdecl_c99_type(Elf64_Word, uint32_t)
cdecl_c99_type(Elf64_Sword, int32_t)

cdecl_c99_type(Elf64_Xword, uint64_t)
cdecl_c99_type(Elf64_Sxword, int64_t)

cdecl_c99_type(Elf64_Addr, uint64_t)

cdecl_c99_type(Elf64_Off, uint64_t)

cdecl_c99_type(Elf64_Section, uint16_t)

cdecl_c99_type(Elf64_Versym, Elf64_Half)

cdecl_type(Elf64_Ehdr)
cdecl_type(Elf64_Shdr)
cdecl_type(Elf64_Dyn)
