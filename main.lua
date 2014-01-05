local ffi = require("ffi")
ffi.cdef[[
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

struct AAssetManager;
typedef struct AAssetManager AAssetManager;

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

-- change this to the path you want to use:
package.path = "/sdcard/luajit-activity/?.lua"

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

function android.handle_input(app, event)
	local ev_type = ffi.C.AInputEvent_getType(event)
	if ev_type == ffi.C.AINPUT_EVENT_TYPE_MOTION then
		local ptr_count = ffi.C.AMotionEvent_getPointerCount(event)
		for i = 0, ptr_count-1 do
			local x = ffi.C.AMotionEvent_getX(event, i)
			local y = ffi.C.AMotionEvent_getY(event, i)
			android.LOGI("motion event at " .. tonumber(x) .. "," .. tonumber(y) .. " for ptr no " .. i)
		end
		return 1
	elseif ev_type == ffi.C.AINPUT_EVENT_TYPE_KEY then
		android.LOGI("got key event, leaving it unhandled")
	end
	return 0
end

function android.draw_frame()
	if android.app.window == nil then
		-- No window.
		return
	end

	local buffer = ffi.new("ANativeWindow_Buffer[1]")
	if ffi.C.ANativeWindow_lock(android.app.window, buffer, nil) < 0 then
		android.LOGW("Unable to lock window buffer");
		return
	end

	local bb = nil
	if buffer[0].format == ffi.C.WINDOW_FORMAT_RGBA_8888
	or buffer[0].format == ffi.C.WINDOW_FORMAT_RGBX_8888
	then
		-- modify buffer[0].bits here
	elseif buffer[0].format == ffi.C.WINDOW_FORMAT_RGB_565 then
		-- modify buffer[0].bits here
	else
		android.LOGE("unsupported window format!")
	end

	ffi.C.ANativeWindow_unlockAndPost(android.app.window);
end

function android.handle_cmd(app, cmd)
	android.LOGI("got command: " .. tonumber(cmd))
	if cmd == ffi.C.APP_CMD_INIT_WINDOW then
		android.draw_frame()
	elseif cmd == ffi.C.APP_CMD_TERM_WINDOW then
		-- do nothing for now
	elseif cmd == ffi.C.APP_CMD_LOST_FOCUS then
		android.draw_frame()
	end
end

function android.init(android_app_state)
	android.app = ffi.cast("struct android_app*", android_app_state)
	--[[ alternative C-based callback-based way (like in Android NDK example):
	android.app.onAppCmd = android.handle_cmd
	android.app.onInputEvent = android.handle_input
	--]]
end

-- you probably want to integrate the following loop into your own
-- event loop (if you have one already).
-- see the Android NDK documentation (the header files, that is!) in order
-- to get an idea how to use ALooper to poll your filedescriptors, too.
function android.loop()
	local events = ffi.new("int[1]")
	local source = ffi.new("struct android_poll_source*[1]")
	while true do
		if ffi.C.ALooper_pollAll(-1, nil, events, ffi.cast("void**", source)) >= 0 then
			if source[0] ~= nil then
				--[[ alternative C-based callback-based way (like in Android NDK example):
				source[0].process(android.app, source[0])
				--]]
				--[[ you can comment out this then: ]]
				if source[0].id == ffi.C.LOOPER_ID_MAIN then
					local cmd = ffi.C.android_app_read_cmd(android.app)
					ffi.C.android_app_pre_exec_cmd(android.app, cmd)
					android.handle_cmd(android.app, cmd)
					ffi.C.android_app_post_exec_cmd(android.app, cmd)
				elseif source[0].id == ffi.C.LOOPER_ID_INPUT then
					local event = ffi.new("AInputEvent*[1]")
					while ffi.C.AInputQueue_getEvent(android.app.inputQueue, event) >= 0 do
						if ffi.C.AInputQueue_preDispatchEvent(android.app.inputQueue, event[0]) == 0 then
							ffi.C.AInputQueue_finishEvent(android.app.inputQueue, event[0],
								android.handle_input(android.app, event[0]))
						end
					end
				end
				--]] up to here
			end
			if android.app.destroyRequested ~= 0 then
				android.LOGI("Engine thread destroy requested!")
				return
			end
		end
	end
end

android.init(android_app_state)
android.loop()
