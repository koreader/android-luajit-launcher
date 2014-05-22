/*
Copyright (c) 2014 Hans-Werner Hilse <software@haveyouseenthiscat.de>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
#include <android_native_app_glue.h>

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dlfcn.h>

#include <linux/elf.h>

#include <android/log.h>
#include <android/asset_manager.h>

#include "luajit-2.0/lua.h"
#include "luajit-2.0/lauxlib.h"

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"luajit-launcher",__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"luajit-launcher",__VA_ARGS__)
#define  LOADER_ASSET "android.lua"

static char* getApplicationDirectory(struct android_app* state);

static const char *library_locations[] = {"/lib", "/system/lib", NULL, NULL, NULL};

void android_main(struct android_app* state) {
	lua_State *L;
	AAsset* luaCode;
	const void *buf;
	off_t bufsize;
	int status;
    char *libs_dir;

	// Make sure glue isn't stripped.
	app_dummy();

    char *base_dir = getApplicationDirectory(state);
    libs_dir = malloc(strlen(base_dir) + 6);
    strcpy(libs_dir, base_dir);
    strcat(libs_dir, "/libs");
    library_locations[2] = base_dir;
    library_locations[3] = libs_dir;

	luaCode = AAssetManager_open(state->activity->assetManager, LOADER_ASSET, AASSET_MODE_BUFFER);
	if (luaCode == NULL) {
		LOGE("error loading loader asset");
		return;
	}

	bufsize = AAsset_getLength(luaCode);
	buf = AAsset_getBuffer(luaCode);
	if (buf == NULL) {
		LOGE("error getting loader asset buffer");
		return;
	}

	// Load initial Lua loader from our asset store:

	L = luaL_newstate();
	luaL_openlibs(L);

	status = luaL_loadbuffer(L, (const char*) buf, (size_t) bufsize, LOADER_ASSET);
	AAsset_close(luaCode);
	if (status) {
		LOGE("error loading file: %s", lua_tostring(L, -1));
		return;
	}

	// pass the android_app state to Lua land:
	lua_pushlightuserdata(L, state);

	status = lua_pcall(L, 1, LUA_MULTRET, 0);
	if (status) {
		LOGE("Failed to run script: %s", lua_tostring(L, -1));
		return;
	}

	lua_close(L);
}

char *getApplicationDirectory(struct android_app* state) {
    // PackageManager packetManager = getPackageManager();
    // String packageName = getPackageName();
    // PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
    // String dataDir = packageInfo.applicationInfo.dataDir;

    // Connect to JVM.
    //

    ANativeActivity* activity = state->activity;
    JavaVM* jvm = state->activity->vm;
    JNIEnv* env = NULL;
    (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
    jint result = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    if (result == JNI_ERR)
    {
        LOGE("Failied to attach to the Java VM's current thread.");
        exit(EXIT_FAILURE);
    }

    // Perform Java calls.
    //

    jclass clazz = (*env)->GetObjectClass(env, activity->clazz);
    jmethodID getPackageManagerID = (*env)->GetMethodID(env, clazz, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jmethodID getPackageNameID = (*env)->GetMethodID(env, clazz, "getPackageName", "()Ljava/lang/String;");

    jobject packageManager = (*env)->CallObjectMethod(env, activity->clazz, getPackageManagerID);
    jobject packageName = (*env)->CallObjectMethod(env, activity->clazz, getPackageNameID);

    clazz = (*env)->GetObjectClass(env, packageManager);
    jmethodID getPackageInfoID = (*env)->GetMethodID(env, clazz, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    jobject packageInfo = (*env)->CallObjectMethod(env, packageManager, getPackageInfoID, packageName, 0);

    clazz = (*env)->GetObjectClass(env, packageInfo);
    jfieldID applicationInfoID = (*env)->GetFieldID(env, clazz, "applicationInfo", "Landroid/content/pm/ApplicationInfo;");
    jobject applicationInfo = (*env)->GetObjectField(env, packageInfo, applicationInfoID);

    clazz = (*env)->GetObjectClass(env, applicationInfo);
    jfieldID dataDirID = (*env)->GetFieldID(env, clazz, "dataDir", "Ljava/lang/String;");
    if (dataDirID == NULL) {
        LOGE("dataDir field not found.");
        return;
    }
    jstring dataDir = (*env)->GetObjectField(env, applicationInfo, dataDirID);

    // Copy string in a custom buffer.
    //

    int strlen = (*env)->GetStringUTFLength(env, dataDir);
    char *buffer = malloc(strlen + 1);
    const char *str = (*env)->GetStringUTFChars(env, dataDir, NULL);
    strcpy(buffer, str);
    buffer[strlen] = '\0';

    (*env)->ReleaseStringUTFChars(env, dataDir, str);

    // Detach from JVM.
    //

    (*jvm)->DetachCurrentThread(jvm);

    return buffer;
}

/*
 * lo_dlopen from libreoffice/core
 * MPL 1.1 / GPLv3+ / LGPLv3+
 */

/* Zip data structures */
/* compression methods */
#define STORE    0
#define DEFLATE  8
#define LZMA    14

struct local_file_header {
    uint32_t signature;
    uint16_t min_version;
    uint16_t general_flag;
    uint16_t compression;
    uint16_t lastmod_time;
    uint16_t lastmod_date;
    uint32_t crc32;
    uint32_t compressed_size;
    uint32_t uncompressed_size;
    uint16_t filename_size;
    uint16_t extra_field_size;
    char     data[0];
} __attribute__((__packed__));

struct cdir_entry {
    uint32_t signature;
    uint16_t creator_version;
    uint16_t min_version;
    uint16_t general_flag;
    uint16_t compression;
    uint16_t lastmod_time;
    uint16_t lastmod_date;
    uint32_t crc32;
    uint32_t compressed_size;
    uint32_t uncompressed_size;
    uint16_t filename_size;
    uint16_t extra_field_size;
    uint16_t file_comment_size;
    uint16_t disk_num;
    uint16_t internal_attr;
    uint32_t external_attr;
    uint32_t offset;
    char     data[0];
} __attribute__((__packed__));

#define CDIR_END_SIG 0x06054b50

struct cdir_end {
    uint32_t signature;
    uint16_t disk_num;
    uint16_t cdir_disk;
    uint16_t disk_entries;
    uint16_t cdir_entries;
    uint32_t cdir_size;
    uint32_t cdir_offset;
    uint16_t comment_size;
    char     comment[0];
} __attribute__((__packed__));

/* End of Zip data structures */

static char * read_section(int fd, Elf32_Shdr *shdr) {
    char *result;

    result = malloc(shdr->sh_size);
    if (lseek(fd, shdr->sh_offset, SEEK_SET) < 0) {
        close(fd);
        free(result);
        return NULL;
    }
    if (read(fd, result, shdr->sh_size) < shdr->sh_size) {
        close(fd);
        free(result);
        return NULL;
    }

    return result;
}

static void free_ptrarray(void **pa) {
    void **rover = pa;

    while (*rover != NULL)
        free(*rover++);

    free(pa);
}

char ** lo_dlneeds(const char *library) {
    int i, fd;
    int n_needed;
    char **result;
    char *shstrtab, *dynstr;
    Elf32_Ehdr hdr;
    Elf32_Shdr shdr;
    Elf32_Dyn dyn;

    /* Open library and read ELF header */

    fd = open(library, O_RDONLY);

    if (fd == -1) {
        LOGE("lo_dlneeds: Could not open library %s: %s", library, strerror(errno));
        return NULL;
    }

    if (read(fd, &hdr, sizeof(hdr)) < sizeof(hdr)) {
        LOGE("lo_dlneeds: Could not read ELF header of %s", library);
        close(fd);
        return NULL;
    }

    /* Read in .shstrtab */

    if (lseek(fd, hdr.e_shoff + hdr.e_shstrndx * sizeof(shdr), SEEK_SET) < 0) {
        LOGE("lo_dlneeds: Could not seek to .shstrtab section header of %s", library);
        close(fd);
        return NULL;
    }
    if (read(fd, &shdr, sizeof(shdr)) < sizeof(shdr)) {
        LOGE("lo_dlneeds: Could not read section header of %s", library);
        close(fd);
        return NULL;
    }

    shstrtab = read_section(fd, &shdr);
    if (shstrtab == NULL)
        return NULL;

    /* Read section headers, looking for .dynstr section */

    if (lseek(fd, hdr.e_shoff, SEEK_SET) < 0) {
        LOGE("lo_dlneeds: Could not seek to section headers of %s", library);
        close(fd);
        return NULL;
    }
    for (i = 0; i < hdr.e_shnum; i++) {
        if (read(fd, &shdr, sizeof(shdr)) < sizeof(shdr)) {
            LOGE("lo_dlneeds: Could not read section header of %s", library);
            close(fd);
            return NULL;
        }
        if (shdr.sh_type == SHT_STRTAB &&
            strcmp(shstrtab + shdr.sh_name, ".dynstr") == 0) {
            dynstr = read_section(fd, &shdr);
            if (dynstr == NULL) {
                free(shstrtab);
                return NULL;
            }
            break;
        }
    }

    if (i == hdr.e_shnum) {
        LOGE("lo_dlneeds: No .dynstr section in %s", library);
        close(fd);
        return NULL;
    }

    /* Read section headers, looking for .dynamic section */

    if (lseek(fd, hdr.e_shoff, SEEK_SET) < 0) {
        LOGE("lo_dlneeds: Could not seek to section headers of %s", library);
        close(fd);
        return NULL;
    }
    for (i = 0; i < hdr.e_shnum; i++) {
        if (read(fd, &shdr, sizeof(shdr)) < sizeof(shdr)) {
            LOGE("lo_dlneeds: Could not read section header of %s", library);
            close(fd);
            return NULL;
        }
        if (shdr.sh_type == SHT_DYNAMIC) {
            int dynoff;
            int *libnames;

            /* Count number of DT_NEEDED entries */
            n_needed = 0;
            if (lseek(fd, shdr.sh_offset, SEEK_SET) < 0) {
                LOGE("lo_dlneeds: Could not seek to .dynamic section of %s", library);
                close(fd);
                return NULL;
            }
            for (dynoff = 0; dynoff < shdr.sh_size; dynoff += sizeof(dyn)) {
                if (read(fd, &dyn, sizeof(dyn)) < sizeof(dyn)) {
                    LOGE("lo_dlneeds: Could not read .dynamic entry of %s", library);
                    close(fd);
                    return NULL;
                }
                if (dyn.d_tag == DT_NEEDED)
                    n_needed++;
            }

            /* LOGI("Found %d DT_NEEDED libs", n_needed); */

            result = malloc((n_needed+1) * sizeof(char *));

            n_needed = 0;
            if (lseek(fd, shdr.sh_offset, SEEK_SET) < 0) {
                LOGE("lo_dlneeds: Could not seek to .dynamic section of %s", library);
                close(fd);
                free(result);
                return NULL;
            }
            for (dynoff = 0; dynoff < shdr.sh_size; dynoff += sizeof(dyn)) {
                if (read(fd, &dyn, sizeof(dyn)) < sizeof(dyn)) {
                    LOGE("lo_dlneeds: Could not read .dynamic entry in %s", library);
                    close(fd);
                    free(result);
                    return NULL;
                }
                if (dyn.d_tag == DT_NEEDED) {
                    /* LOGI("needs: %s\n", dynstr + dyn.d_un.d_val); */
                    result[n_needed] = strdup(dynstr + dyn.d_un.d_val);
                    n_needed++;
                }
            }

            close(fd);
            free(dynstr);
            free(shstrtab);
            result[n_needed] = NULL;
            return result;
        }
    }

    LOGE("lo_dlneeds: Could not find .dynamic section in %s", library);
    close(fd);
    return NULL;
}

void * lo_dlopen(const char *library) {
    /*
     * We should *not* try to just dlopen() the bare library name
     * first, as the stupid dynamic linker remembers for each library
     * basename if loading it has failed. Thus if you try loading it
     * once, and it fails because of missing needed libraries, and
     * your load those, and then try again, it fails with an
     * infuriating message "failed to load previously" in the log.
     *
     * We *must* first dlopen() all needed libraries, recursively. It
     * shouldn't matter if we dlopen() a library that already is
     * loaded, dlopen() just returns the same value then.
     */

    typedef struct loadedLib {
        const char *name;
        void *handle;
        struct loadedLib *next;
    } *loadedLib;
    static loadedLib loaded_libraries = NULL;

    loadedLib rover;
    loadedLib new_loaded_lib;

    struct stat st;
    void *p;
    char *full_name;
    char **needed;
    int i;
    int found;

    rover = loaded_libraries;
    while (rover != NULL &&
           strcmp(rover->name, library) != 0)
        rover = rover->next;

    if (rover != NULL)
        return rover->handle;

    /* LOGI("lo_dlopen(%s)", library); */

    found = 0;
    if (library[0] == '/') {
        full_name = strdup(library);

        if (stat(full_name, &st) == 0 &&
            S_ISREG(st.st_mode))
            found = 1;
        else
            free(full_name);
    } else {
        for (i = 0; !found && library_locations[i] != NULL; i++) {
            full_name = malloc(strlen(library_locations[i]) + 1 + strlen(library) + 1);
            strcpy(full_name, library_locations[i]);
            strcat(full_name, "/");
            strcat(full_name, library);

            if (stat(full_name, &st) == 0 &&
                S_ISREG(st.st_mode))
                found = 1;
            else
                free(full_name);
        }
    }

    if (!found) {
        LOGE("lo_dlopen: Library %s not found", library);
        return NULL;
    }

    needed = lo_dlneeds(full_name);
    if (needed == NULL) {
        free(full_name);
        return NULL;
    }

    for (i = 0; needed[i] != NULL; i++) {
        if (lo_dlopen(needed[i]) == NULL) {
            free_ptrarray((void **) needed);
            free(full_name);
            return NULL;
        }
    }
    free_ptrarray((void **) needed);

    p = dlopen(full_name, RTLD_LOCAL);
    LOGI("dlopen(%s) = %p", full_name, p);
    free(full_name);
    if (p == NULL)
        LOGE("lo_dlopen: Error from dlopen(%s): %s", library, dlerror());

    new_loaded_lib = malloc(sizeof(*new_loaded_lib));
    new_loaded_lib->name = strdup(library);
    new_loaded_lib->handle = p;

    new_loaded_lib->next = loaded_libraries;
    loaded_libraries = new_loaded_lib;

    return p;
}

