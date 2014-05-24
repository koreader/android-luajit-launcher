local ffi = require("ffi")
ffi.cdef[[
// dlopen that loads library recursiviely
void * lo_dlopen(char *library);

// logging:
int __android_log_print(int prio, const char *tag,  const char *fmt, ...);
typedef enum android_LogPriority {
    ANDROID_LOG_UNKNOWN = 0,
    ANDROID_LOG_DEFAULT,
    ANDROID_LOG_VERBOSE,
    ANDROID_LOG_DEBUG,
    ANDROID_LOG_INFO,
    ANDROID_LOG_WARN,
    ANDROID_LOG_ERROR,
    ANDROID_LOG_FATAL,
    ANDROID_LOG_SILENT,
} android_LogPriority;

// boilerplate, to be expanded when needed:

struct AConfiguration;
typedef struct AConfiguration AConfiguration;


// from android-ndk/platforms/android-9/arch-arm/usr/include/asm/posix_types.h:
typedef long __kernel_off_t;

// from android-ndk/platforms/android-9/arch-arm/usr/include/sys/types.h:
typedef __kernel_off_t off_t;

// from android-ndk/platforms/android-9/arch-arm/usr/include/android/asset_manager.h:

struct AAssetManager;
typedef struct AAssetManager AAssetManager;

struct AAssetDir;
typedef struct AAssetDir AAssetDir;

struct AAsset;
typedef struct AAsset AAsset;

enum {
    AASSET_MODE_UNKNOWN      = 0,
    AASSET_MODE_RANDOM       = 1,
    AASSET_MODE_STREAMING    = 2,
    AASSET_MODE_BUFFER       = 3
};

AAssetDir* AAssetManager_openDir(AAssetManager* mgr, const char* dirName);
AAsset* AAssetManager_open(AAssetManager* mgr, const char* filename, int mode);
const char* AAssetDir_getNextFileName(AAssetDir* assetDir);
void AAssetDir_rewind(AAssetDir* assetDir);
void AAssetDir_close(AAssetDir* assetDir);
int AAsset_read(AAsset* asset, void* buf, size_t count);
off_t AAsset_seek(AAsset* asset, off_t offset, int whence);
void AAsset_close(AAsset* asset);
const void* AAsset_getBuffer(AAsset* asset);
off_t AAsset_getLength(AAsset* asset);
off_t AAsset_getRemainingLength(AAsset* asset);
int AAsset_openFileDescriptor(AAsset* asset, off_t* outStart, off_t* outLength);
int AAsset_isAllocated(AAsset* asset);


// from android-ndk/platforms/android-9/arch-arm/usr/include/android/looper.h:

struct ALooper;
typedef struct ALooper ALooper;

enum {
    ALOOPER_POLL_WAKE = -1,
    ALOOPER_POLL_CALLBACK = -2,
    ALOOPER_POLL_TIMEOUT = -3,
    ALOOPER_POLL_ERROR = -4,
};

enum {
    ALOOPER_EVENT_INPUT = 1 << 0,
    ALOOPER_EVENT_OUTPUT = 1 << 1,
    ALOOPER_EVENT_ERROR = 1 << 2,
    ALOOPER_EVENT_HANGUP = 1 << 3,
    ALOOPER_EVENT_INVALID = 1 << 4,
};

typedef int (*ALooper_callbackFunc)(int fd, int events, void* data);

int ALooper_pollAll(int timeoutMillis, int* outFd, int* outEvents, void** outData);
int ALooper_pollOnce(int timeoutMillis, int* outFd, int* outEvents, void** outData);

// from android-ndk/platforms/android-9/arch-arm/usr/include/android/input.h:

enum {
    AKEY_STATE_UNKNOWN = -1,
    AKEY_STATE_UP = 0,
    AKEY_STATE_DOWN = 1,
    AKEY_STATE_VIRTUAL = 2
};

enum {
    AMETA_NONE = 0,
    AMETA_ALT_ON = 0x02,
    AMETA_ALT_LEFT_ON = 0x10,
    AMETA_ALT_RIGHT_ON = 0x20,
    AMETA_SHIFT_ON = 0x01,
    AMETA_SHIFT_LEFT_ON = 0x40,
    AMETA_SHIFT_RIGHT_ON = 0x80,
    AMETA_SYM_ON = 0x04
};

struct AInputEvent;
typedef struct AInputEvent AInputEvent;

enum {
    AINPUT_EVENT_TYPE_KEY = 1,
    AINPUT_EVENT_TYPE_MOTION = 2
};

enum {
    AKEY_EVENT_ACTION_DOWN = 0,
    AKEY_EVENT_ACTION_UP = 1,
    AKEY_EVENT_ACTION_MULTIPLE = 2
};

enum {
    AKEY_EVENT_FLAG_WOKE_HERE = 0x1,
    AKEY_EVENT_FLAG_SOFT_KEYBOARD = 0x2,
    AKEY_EVENT_FLAG_KEEP_TOUCH_MODE = 0x4,
    AKEY_EVENT_FLAG_FROM_SYSTEM = 0x8,
    AKEY_EVENT_FLAG_EDITOR_ACTION = 0x10,
    AKEY_EVENT_FLAG_CANCELED = 0x20,
    AKEY_EVENT_FLAG_VIRTUAL_HARD_KEY = 0x40,
    AKEY_EVENT_FLAG_LONG_PRESS = 0x80,
    AKEY_EVENT_FLAG_CANCELED_LONG_PRESS = 0x100,
    AKEY_EVENT_FLAG_TRACKING = 0x200
};
static const int AMOTION_EVENT_ACTION_POINTER_INDEX_SHIFT = 8;

enum {
    AMOTION_EVENT_ACTION_MASK = 0xff,
    AMOTION_EVENT_ACTION_POINTER_INDEX_MASK  = 0xff00,
    AMOTION_EVENT_ACTION_DOWN = 0,
    AMOTION_EVENT_ACTION_UP = 1,
    AMOTION_EVENT_ACTION_MOVE = 2,
    AMOTION_EVENT_ACTION_CANCEL = 3,
    AMOTION_EVENT_ACTION_OUTSIDE = 4,
    AMOTION_EVENT_ACTION_POINTER_DOWN = 5,
    AMOTION_EVENT_ACTION_POINTER_UP = 6
};

enum {
    AMOTION_EVENT_FLAG_WINDOW_IS_OBSCURED = 0x1,
};

enum {
    AMOTION_EVENT_EDGE_FLAG_NONE = 0,
    AMOTION_EVENT_EDGE_FLAG_TOP = 0x01,
    AMOTION_EVENT_EDGE_FLAG_BOTTOM = 0x02,
    AMOTION_EVENT_EDGE_FLAG_LEFT = 0x04,
    AMOTION_EVENT_EDGE_FLAG_RIGHT = 0x08
};

enum {
    AINPUT_SOURCE_CLASS_MASK = 0x000000ff,
    AINPUT_SOURCE_CLASS_BUTTON = 0x00000001,
    AINPUT_SOURCE_CLASS_POINTER = 0x00000002,
    AINPUT_SOURCE_CLASS_NAVIGATION = 0x00000004,
    AINPUT_SOURCE_CLASS_POSITION = 0x00000008,
};

enum {
    AINPUT_SOURCE_UNKNOWN = 0x00000000,
    AINPUT_SOURCE_KEYBOARD = 0x00000100 | AINPUT_SOURCE_CLASS_BUTTON,
    AINPUT_SOURCE_DPAD = 0x00000200 | AINPUT_SOURCE_CLASS_BUTTON,
    AINPUT_SOURCE_TOUCHSCREEN = 0x00001000 | AINPUT_SOURCE_CLASS_POINTER,
    AINPUT_SOURCE_MOUSE = 0x00002000 | AINPUT_SOURCE_CLASS_POINTER,
    AINPUT_SOURCE_TRACKBALL = 0x00010000 | AINPUT_SOURCE_CLASS_NAVIGATION,
    AINPUT_SOURCE_TOUCHPAD = 0x00100000 | AINPUT_SOURCE_CLASS_POSITION,
    AINPUT_SOURCE_ANY = 0xffffff00,
};

enum {
    AINPUT_KEYBOARD_TYPE_NONE = 0,
    AINPUT_KEYBOARD_TYPE_NON_ALPHABETIC = 1,
    AINPUT_KEYBOARD_TYPE_ALPHABETIC = 2,
};

enum {
    AINPUT_MOTION_RANGE_X = 0,
    AINPUT_MOTION_RANGE_Y = 1,
    AINPUT_MOTION_RANGE_PRESSURE = 2,
    AINPUT_MOTION_RANGE_SIZE = 3,
    AINPUT_MOTION_RANGE_TOUCH_MAJOR = 4,
    AINPUT_MOTION_RANGE_TOUCH_MINOR = 5,
    AINPUT_MOTION_RANGE_TOOL_MAJOR = 6,
    AINPUT_MOTION_RANGE_TOOL_MINOR = 7,
    AINPUT_MOTION_RANGE_ORIENTATION = 8,
};

int32_t AInputEvent_getType(const AInputEvent* event);
int32_t AInputEvent_getDeviceId(const AInputEvent* event);
int32_t AInputEvent_getSource(const AInputEvent* event);
int32_t AKeyEvent_getAction(const AInputEvent* key_event);
int32_t AKeyEvent_getFlags(const AInputEvent* key_event);
int32_t AKeyEvent_getKeyCode(const AInputEvent* key_event);
int32_t AKeyEvent_getScanCode(const AInputEvent* key_event);
int32_t AKeyEvent_getMetaState(const AInputEvent* key_event);
int32_t AKeyEvent_getRepeatCount(const AInputEvent* key_event);
int64_t AKeyEvent_getDownTime(const AInputEvent* key_event);
int64_t AKeyEvent_getEventTime(const AInputEvent* key_event);
int32_t AMotionEvent_getAction(const AInputEvent* motion_event);
int32_t AMotionEvent_getFlags(const AInputEvent* motion_event);
int32_t AMotionEvent_getMetaState(const AInputEvent* motion_event);
int32_t AMotionEvent_getEdgeFlags(const AInputEvent* motion_event);
int64_t AMotionEvent_getDownTime(const AInputEvent* motion_event);
int64_t AMotionEvent_getEventTime(const AInputEvent* motion_event);
float AMotionEvent_getXOffset(const AInputEvent* motion_event); //__NDK_FPABI__
float AMotionEvent_getYOffset(const AInputEvent* motion_event); //__NDK_FPABI__
float AMotionEvent_getXPrecision(const AInputEvent* motion_event); //__NDK_FPABI__
float AMotionEvent_getYPrecision(const AInputEvent* motion_event); //__NDK_FPABI__
size_t AMotionEvent_getPointerCount(const AInputEvent* motion_event);
int32_t AMotionEvent_getPointerId(const AInputEvent* motion_event, size_t pointer_index);
float AMotionEvent_getRawX(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getRawY(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getX(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getY(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getPressure(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getSize(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getTouchMajor(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getTouchMinor(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getToolMajor(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getToolMinor(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
float AMotionEvent_getOrientation(const AInputEvent* motion_event, size_t pointer_index); //__NDK_FPABI__
size_t AMotionEvent_getHistorySize(const AInputEvent* motion_event);
int64_t AMotionEvent_getHistoricalEventTime(const AInputEvent* motion_event,
        size_t history_index);
float AMotionEvent_getHistoricalRawX(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalRawY(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalX(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalY(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalPressure(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalSize(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalTouchMajor(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalTouchMinor(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalToolMajor(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalToolMinor(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__
float AMotionEvent_getHistoricalOrientation(const AInputEvent* motion_event, size_t pointer_index,
        size_t history_index); //__NDK_FPABI__

struct AInputQueue;
typedef struct AInputQueue AInputQueue;

void AInputQueue_attachLooper(AInputQueue* queue, ALooper* looper,
        int ident, ALooper_callbackFunc callback, void* data);
void AInputQueue_detachLooper(AInputQueue* queue);
int32_t AInputQueue_hasEvents(AInputQueue* queue);
int32_t AInputQueue_getEvent(AInputQueue* queue, AInputEvent** outEvent);
int32_t AInputQueue_preDispatchEvent(AInputQueue* queue, AInputEvent* event);
void AInputQueue_finishEvent(AInputQueue* queue, AInputEvent* event, int handled);

// from android-ndk/platforms/android-9/arch-arm/usr/include/android/rect.h:

typedef struct ARect {
    int32_t left;
    int32_t top;
    int32_t right;
    int32_t bottom;
} ARect;

// from android-ndk/platforms/android-9/arch-arm/usr/include/android/native_window.h:

enum {
    WINDOW_FORMAT_RGBA_8888          = 1,
    WINDOW_FORMAT_RGBX_8888          = 2,
    WINDOW_FORMAT_RGB_565            = 4,
};

struct ANativeWindow;
typedef struct ANativeWindow ANativeWindow;

typedef struct ANativeWindow_Buffer {
    int32_t width;
    int32_t height;
    int32_t stride;
    int32_t format;
    void* bits;
    uint32_t reserved[6];
} ANativeWindow_Buffer;

void ANativeWindow_acquire(ANativeWindow* window);
void ANativeWindow_release(ANativeWindow* window);
int32_t ANativeWindow_getWidth(ANativeWindow* window);
int32_t ANativeWindow_getHeight(ANativeWindow* window);
int32_t ANativeWindow_getFormat(ANativeWindow* window);
int32_t ANativeWindow_setBuffersGeometry(ANativeWindow* window, int32_t width, int32_t height, int32_t format);
int32_t ANativeWindow_lock(ANativeWindow* window, ANativeWindow_Buffer* outBuffer,
        ARect* inOutDirtyBounds);
int32_t ANativeWindow_unlockAndPost(ANativeWindow* window);

// from android-ndk/platforms/android-9/arch-x86/usr/include/jni.h:

struct JNIInvokeInterface;
typedef const struct JNIInvokeInterface* JavaVM;
struct JNINativeInterface;
typedef const struct JNINativeInterface* JNIEnv;
typedef void* jobject;

// from android-ndk/platforms/android-9/arch-x86/usr/include/android/native_activity.h:

struct ANativeActivityCallbacks;

typedef struct ANativeActivity {
    struct ANativeActivityCallbacks* callbacks;
    JavaVM* vm;
    JNIEnv* env;
    jobject clazz;
    const char* internalDataPath;
    const char* externalDataPath;
    int32_t sdkVersion;
    void* instance;
    AAssetManager* assetManager;
} ANativeActivity;

// from android-ndk/platforms/android-9/arch-arm/usr/include/pthread.h:

typedef struct {
    int volatile value;
} pthread_mutex_t;

typedef struct {
    int volatile value;
} pthread_cond_t;

typedef long pthread_t;

// from android-ndk/sources/android/native_app_glue/android_native_app_glue.h:

struct android_poll_source {
    int32_t id;
    struct android_app* app;
    void (*process)(struct android_app* app, struct android_poll_source* source);
};

struct android_app {
    void* userData;
    void (*onAppCmd)(struct android_app* app, int32_t cmd);
    int32_t (*onInputEvent)(struct android_app* app, AInputEvent* event);
    ANativeActivity* activity;
    AConfiguration* config;
    void* savedState;
    size_t savedStateSize;
    ALooper* looper;
    AInputQueue* inputQueue;
    ANativeWindow* window;
    ARect contentRect;
    int activityState;
    int destroyRequested;

    // -------------------------------------------------
    // Below are "private" implementation of the glue code.

    pthread_mutex_t mutex;
    pthread_cond_t cond;

    int msgread;
    int msgwrite;

    pthread_t thread;

    struct android_poll_source cmdPollSource;
    struct android_poll_source inputPollSource;

    int running;
    int stateSaved;
    int destroyed;
    int redrawNeeded;
    AInputQueue* pendingInputQueue;
    ANativeWindow* pendingWindow;
    ARect pendingContentRect;
};

enum {
    LOOPER_ID_MAIN = 1,
    LOOPER_ID_INPUT = 2,
    LOOPER_ID_USER = 3,
};

enum {
    APP_CMD_INPUT_CHANGED,
    APP_CMD_INIT_WINDOW,
    APP_CMD_TERM_WINDOW,
    APP_CMD_WINDOW_RESIZED,
    APP_CMD_WINDOW_REDRAW_NEEDED,
    APP_CMD_CONTENT_RECT_CHANGED,
    APP_CMD_GAINED_FOCUS,
    APP_CMD_LOST_FOCUS,
    APP_CMD_CONFIG_CHANGED,
    APP_CMD_LOW_MEMORY,
    APP_CMD_START,
    APP_CMD_RESUME,
    APP_CMD_SAVE_STATE,
    APP_CMD_PAUSE,
    APP_CMD_STOP,
    APP_CMD_DESTROY,
};

int8_t android_app_read_cmd(struct android_app* android_app);
void android_app_pre_exec_cmd(struct android_app* android_app, int8_t cmd);
void android_app_post_exec_cmd(struct android_app* android_app, int8_t cmd);
]]

local android = {
    app = nil,
    log_name = "luajit-launcher",
}

function android.LOG(level, message)
    ffi.C.__android_log_print(level, android.log_name, "%s", message)
end
function android.LOGV(message)
    android.LOG(ffi.C.ANDROID_LOG_VERBOSE, message)
end
function android.LOGI(message)
    android.LOG(ffi.C.ANDROID_LOG_INFO, message)
end
function android.LOGW(message)
    android.LOG(ffi.C.ANDROID_LOG_WARN, message)
end
function android.LOGE(message)
    android.LOG(ffi.C.ANDROID_LOG_ERROR, message)
end

--[[
a loader function for Lua which will look for assets when loading modules
--]]
function android.asset_loader(modulename)
    local errmsg = ""
    -- Find source
    local modulepath = string.gsub(modulename, "%.", "/")
    for path in string.gmatch(package.path, "([^;]+)") do
        local filename = string.gsub(path, "%?", modulepath)
        local asset = ffi.C.AAssetManager_open(
            android.app.activity.assetManager,
            filename, ffi.C.AASSET_MODE_BUFFER)
        --android.LOGI(string.format("trying to open asset %s: %s", filename, tostring(asset)))
        if asset ~= nil then
            -- read asset:
            local assetdata = ffi.C.AAsset_getBuffer(asset)
            local assetsize = ffi.C.AAsset_getLength(asset)
            if assetdata ~= nil then
                -- Compile and return the module
                local compiled = assert(loadstring(ffi.string(assetdata, assetsize), filename))
                ffi.C.AAsset_close(asset)
                return compiled
            else
                ffi.C.AAsset_close(asset)
                errmsg = errmsg.."\n\tunaccessible file '"..filename.."' (tried with asset loader)"
            end
        else
            errmsg = errmsg.."\n\tno file '"..filename.."' (checked with asset loader)"
        end
    end
    return errmsg
end

--[[
this loader function just loads dependency libraries for C module
--]]
function android.deplib_loader(modulename)
    local modulepath = string.gsub(modulename, "%.", "/")
    for path in string.gmatch(package.cpath, "([^;]+)") do
        local module = string.gsub(path, "%?", modulepath)
        -- load dependencies of this module with lo_dlopen
        ffi.C.lo_dlopen(ffi.cast("char*", module))
    end
end
--[[
the C code will call this function:
--]]
local function run(android_app_state, app_data_dir)
    android.app = ffi.cast("struct android_app*", android_app_state)
    android.dir = app_data_dir
    android.LOGI("Application data directory "..android.dir)

    -- set up a sensible package.path
    package.path = "?.lua;"
    -- set absolute cpath
    package.cpath = "?.so;"
    -- register the asset loader
    table.insert(package.loaders, 2, android.asset_loader)
    -- register the dependency lib loader
    table.insert(package.loaders, 3, android.deplib_loader)

    -- register the "android" module
    package.loaded.android = android

    -- ffi.load wrapper
    local ffi_load = ffi.load
    ffi.load = function(library, ...)
        ffi.C.lo_dlopen(ffi.cast("char*", library))
        return ffi_load(library)
    end

    -- add installer module that should install native libraries into libs
    local install = android.asset_loader("install")
    if type(install) == "function" then
        install()
    else
        error("error loading install.lua")
    end
    local main = android.asset_loader("main")
    if type(main) == "function" then
        return main()
    else
        error("error loading main.lua")
    end
end

run(...)
