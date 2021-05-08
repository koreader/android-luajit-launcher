--[[--
Java Native Interface (JNI) wrapper.

@module android
]]

-- Attempt to grab the full maxmcode region in one go on startup,
-- to avoid mcode_alloc failures later on at runtime...
-- c.f., https://www.freelists.org/post/luajit/Android-performance-drop-moving-from-LuaJIT201-LuaJIT202
-- For optimal behavior, this relies on a few LuaJIT hacks:
--   * Ensuring a flush doesn't unmap the mcarea, but only clears it,
--     because when the mcarea is filled, LuaJIT flushes it,
--     and the Lua blitter can happily require more than 256K to flip a CRe page,
--     so flushes are common.
--   * Reserving a 1MB area inside LuaJIT's address space via a global array
--   * Making the first mcode_alloc use the address of this global via MAP_FIXED
-- c.f., koreader-luajit-mcode-reserve-hack.patch
-- Upstream issue: https://github.com/LuaJIT/LuaJIT/issues/285

-- Given that, force the allocation of a single 512K segment *right now*.
-- (LuaJIT defaults are 32, 512 on 32-bit platforms, and 64, 512 otherwise).
jit.opt.start("sizemcode=512", "maxmcode=512")
-- This ensures a trace is generated, which requires an mcarea alloc ;).
for _ = 1, 100 do end

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

enum {
    AKEYCODE_UNKNOWN = 0,
    AKEYCODE_SOFT_LEFT = 1,
    AKEYCODE_SOFT_RIGHT = 2,
    AKEYCODE_HOME = 3,
    AKEYCODE_BACK = 4,
    AKEYCODE_CALL = 5,
    AKEYCODE_ENDCALL = 6,
    AKEYCODE_0 = 7,
    AKEYCODE_1 = 8,
    AKEYCODE_2 = 9,
    AKEYCODE_3 = 10,
    AKEYCODE_4 = 11,
    AKEYCODE_5 = 12,
    AKEYCODE_6 = 13,
    AKEYCODE_7 = 14,
    AKEYCODE_8 = 15,
    AKEYCODE_9 = 16,
    AKEYCODE_STAR = 17,
    AKEYCODE_POUND = 18,
    AKEYCODE_DPAD_UP = 19,
    AKEYCODE_DPAD_DOWN = 20,
    AKEYCODE_DPAD_LEFT = 21,
    AKEYCODE_DPAD_RIGHT = 22,
    AKEYCODE_DPAD_CENTER = 23,
    AKEYCODE_VOLUME_UP = 24,
    AKEYCODE_VOLUME_DOWN = 25,
    AKEYCODE_POWER = 26,
    AKEYCODE_CAMERA = 27,
    AKEYCODE_CLEAR = 28,
    AKEYCODE_A = 29,
    AKEYCODE_B = 30,
    AKEYCODE_C = 31,
    AKEYCODE_D = 32,
    AKEYCODE_E = 33,
    AKEYCODE_F = 34,
    AKEYCODE_G = 35,
    AKEYCODE_H = 36,
    AKEYCODE_I = 37,
    AKEYCODE_J = 38,
    AKEYCODE_K = 39,
    AKEYCODE_L = 40,
    AKEYCODE_M = 41,
    AKEYCODE_N = 42,
    AKEYCODE_O = 43,
    AKEYCODE_P = 44,
    AKEYCODE_Q = 45,
    AKEYCODE_R = 46,
    AKEYCODE_S = 47,
    AKEYCODE_T = 48,
    AKEYCODE_U = 49,
    AKEYCODE_V = 50,
    AKEYCODE_W = 51,
    AKEYCODE_X = 52,
    AKEYCODE_Y = 53,
    AKEYCODE_Z = 54,
    AKEYCODE_COMMA = 55,
    AKEYCODE_PERIOD = 56,
    AKEYCODE_ALT_LEFT = 57,
    AKEYCODE_ALT_RIGHT = 58,
    AKEYCODE_SHIFT_LEFT = 59,
    AKEYCODE_SHIFT_RIGHT = 60,
    AKEYCODE_TAB = 61,
    AKEYCODE_SPACE = 62,
    AKEYCODE_SYM = 63,
    AKEYCODE_EXPLORER = 64,
    AKEYCODE_ENVELOPE = 65,
    AKEYCODE_ENTER = 66,
    AKEYCODE_DEL = 67,
    AKEYCODE_GRAVE = 68,
    AKEYCODE_MINUS = 69,
    AKEYCODE_EQUALS = 70,
    AKEYCODE_LEFT_BRACKET = 71,
    AKEYCODE_RIGHT_BRACKET = 72,
    AKEYCODE_BACKSLASH = 73,
    AKEYCODE_SEMICOLON = 74,
    AKEYCODE_APOSTROPHE = 75,
    AKEYCODE_SLASH = 76,
    AKEYCODE_AT = 77,
    AKEYCODE_NUM = 78,
    AKEYCODE_HEADSETHOOK = 79,
    AKEYCODE_FOCUS = 80,
    AKEYCODE_PLUS = 81,
    AKEYCODE_MENU = 82,
    AKEYCODE_NOTIFICATION = 83,
    AKEYCODE_SEARCH = 84,
    AKEYCODE_MEDIA_PLAY_PAUSE = 85,
    AKEYCODE_MEDIA_STOP = 86,
    AKEYCODE_MEDIA_NEXT = 87,
    AKEYCODE_MEDIA_PREVIOUS = 88,
    AKEYCODE_MEDIA_REWIND = 89,
    AKEYCODE_MEDIA_FAST_FORWARD = 90,
    AKEYCODE_MUTE = 91,
    AKEYCODE_PAGE_UP = 92,
    AKEYCODE_PAGE_DOWN = 93,
    AKEYCODE_PICTSYMBOLS     = 94,
    AKEYCODE_SWITCH_CHARSET  = 95,
    AKEYCODE_BUTTON_A        = 96,
    AKEYCODE_BUTTON_B        = 97,
    AKEYCODE_BUTTON_C        = 98,
    AKEYCODE_BUTTON_X        = 99,
    AKEYCODE_BUTTON_Y        = 100,
    AKEYCODE_BUTTON_Z        = 101,
    AKEYCODE_BUTTON_L1       = 102,
    AKEYCODE_BUTTON_R1       = 103,
    AKEYCODE_BUTTON_L2       = 104,
    AKEYCODE_BUTTON_R2       = 105,
    AKEYCODE_BUTTON_THUMBL   = 106,
    AKEYCODE_BUTTON_THUMBR   = 107,
    AKEYCODE_BUTTON_START    = 108,
    AKEYCODE_BUTTON_SELECT   = 109,
    AKEYCODE_BUTTON_MODE     = 110,
    AKEYCODE_ESCAPE          = 111,
    AKEYCODE_FORWARD_DEL     = 112,
    AKEYCODE_CTRL_LEFT       = 113,
    AKEYCODE_CTRL_RIGHT      = 114,
    AKEYCODE_CAPS_LOCK       = 115,
    AKEYCODE_SCROLL_LOCK     = 116,
    AKEYCODE_META_LEFT       = 117,
    AKEYCODE_META_RIGHT      = 118,
    AKEYCODE_FUNCTION        = 119,
    AKEYCODE_SYSRQ           = 120,
    AKEYCODE_BREAK           = 121,
    AKEYCODE_MOVE_HOME       = 122,
    AKEYCODE_MOVE_END        = 123,
    AKEYCODE_INSERT          = 124,
    AKEYCODE_FORWARD         = 125,
    AKEYCODE_MEDIA_PLAY      = 126,
    AKEYCODE_MEDIA_PAUSE     = 127,
    AKEYCODE_MEDIA_CLOSE     = 128,
    AKEYCODE_MEDIA_EJECT     = 129,
    AKEYCODE_MEDIA_RECORD    = 130,
    AKEYCODE_F1              = 131,
    AKEYCODE_F2              = 132,
    AKEYCODE_F3              = 133,
    AKEYCODE_F4              = 134,
    AKEYCODE_F5              = 135,
    AKEYCODE_F6              = 136,
    AKEYCODE_F7              = 137,
    AKEYCODE_F8              = 138,
    AKEYCODE_F9              = 139,
    AKEYCODE_F10             = 140,
    AKEYCODE_F11             = 141,
    AKEYCODE_F12             = 142,
    AKEYCODE_NUM_LOCK        = 143,
    AKEYCODE_NUMPAD_0        = 144,
    AKEYCODE_NUMPAD_1        = 145,
    AKEYCODE_NUMPAD_2        = 146,
    AKEYCODE_NUMPAD_3        = 147,
    AKEYCODE_NUMPAD_4        = 148,
    AKEYCODE_NUMPAD_5        = 149,
    AKEYCODE_NUMPAD_6        = 150,
    AKEYCODE_NUMPAD_7        = 151,
    AKEYCODE_NUMPAD_8        = 152,
    AKEYCODE_NUMPAD_9        = 153,
    AKEYCODE_NUMPAD_DIVIDE   = 154,
    AKEYCODE_NUMPAD_MULTIPLY = 155,
    AKEYCODE_NUMPAD_SUBTRACT = 156,
    AKEYCODE_NUMPAD_ADD      = 157,
    AKEYCODE_NUMPAD_DOT      = 158,
    AKEYCODE_NUMPAD_COMMA    = 159,
    AKEYCODE_NUMPAD_ENTER    = 160,
    AKEYCODE_NUMPAD_EQUALS   = 161,
    AKEYCODE_NUMPAD_LEFT_PAREN = 162,
    AKEYCODE_NUMPAD_RIGHT_PAREN = 163,
    AKEYCODE_VOLUME_MUTE     = 164,
    AKEYCODE_INFO            = 165,
    AKEYCODE_CHANNEL_UP      = 166,
    AKEYCODE_CHANNEL_DOWN    = 167,
    AKEYCODE_ZOOM_IN         = 168,
    AKEYCODE_ZOOM_OUT        = 169,
    AKEYCODE_TV              = 170,
    AKEYCODE_WINDOW          = 171,
    AKEYCODE_GUIDE           = 172,
    AKEYCODE_DVR             = 173,
    AKEYCODE_BOOKMARK        = 174,
    AKEYCODE_CAPTIONS        = 175,
    AKEYCODE_SETTINGS        = 176,
    AKEYCODE_TV_POWER        = 177,
    AKEYCODE_TV_INPUT        = 178,
    AKEYCODE_STB_POWER       = 179,
    AKEYCODE_STB_INPUT       = 180,
    AKEYCODE_AVR_POWER       = 181,
    AKEYCODE_AVR_INPUT       = 182,
    AKEYCODE_PROG_RED        = 183,
    AKEYCODE_PROG_GREEN      = 184,
    AKEYCODE_PROG_YELLOW     = 185,
    AKEYCODE_PROG_BLUE       = 186,
    AKEYCODE_APP_SWITCH      = 187,
    AKEYCODE_BUTTON_1        = 188,
    AKEYCODE_BUTTON_2        = 189,
    AKEYCODE_BUTTON_3        = 190,
    AKEYCODE_BUTTON_4        = 191,
    AKEYCODE_BUTTON_5        = 192,
    AKEYCODE_BUTTON_6        = 193,
    AKEYCODE_BUTTON_7        = 194,
    AKEYCODE_BUTTON_8        = 195,
    AKEYCODE_BUTTON_9        = 196,
    AKEYCODE_BUTTON_10       = 197,
    AKEYCODE_BUTTON_11       = 198,
    AKEYCODE_BUTTON_12       = 199,
    AKEYCODE_BUTTON_13       = 200,
    AKEYCODE_BUTTON_14       = 201,
    AKEYCODE_BUTTON_15       = 202,
    AKEYCODE_BUTTON_16       = 203,
    AKEYCODE_LANGUAGE_SWITCH = 204,
    AKEYCODE_MANNER_MODE     = 205,
    AKEYCODE_3D_MODE         = 206,
    AKEYCODE_CONTACTS        = 207,
    AKEYCODE_CALENDAR        = 208,
    AKEYCODE_MUSIC           = 209,
    AKEYCODE_CALCULATOR      = 210,
    AKEYCODE_ZENKAKU_HANKAKU = 211,
    AKEYCODE_EISU            = 212,
    AKEYCODE_MUHENKAN        = 213,
    AKEYCODE_HENKAN          = 214,
    AKEYCODE_KATAKANA_HIRAGANA = 215,
    AKEYCODE_YEN             = 216,
    AKEYCODE_RO              = 217,
    AKEYCODE_KANA            = 218,
    AKEYCODE_ASSIST          = 219,
    AKEYCODE_BRIGHTNESS_DOWN = 220,
    AKEYCODE_BRIGHTNESS_UP   = 221,
    AKEYCODE_MEDIA_AUDIO_TRACK = 222,
};

enum {
    AKEEP_SCREEN_ON_ENABLED = -1,
    AKEEP_SCREEN_ON_DISABLED = 0,
};

enum {
    AHAPTIC_LONG_PRESS = 0,
    AHAPTIC_VIRTUAL_KEY = 1,
    AHAPTIC_KEYBOARD_TAP = 3,
    AHAPTIC_CLOCK_TICK = 4,
    AHAPTIC_CONTEXT_CLICK = 6,
    AHAPTIC_KEYBOARD_RELEASE = 7,
    AHAPTIC_VIRTUAL_KEY_RELEASE = 8,
    AHAPTIC_TEXT_HANDLE_MOVE = 9,
};

enum {
    ANETWORK_NONE = 0,
    ANETWORK_WIFI = 1,
    ANETWORK_MOBILE = 2,
    ANETWORK_ETHERNET = 3,
    ANETWORK_BLUETOOTH = 4,
    ANETWORK_VPN = 5,
};

enum {
    ALIGHTS_DIALOG_CLOSED = -1,
    ALIGHTS_DIALOG_OPENED = 0,
    ALIGHTS_DIALOG_CANCEL = 1,
    ALIGHTS_DIALOG_OK = 2,
};

enum {
    ASCREEN_ORIENTATION_UNSPECIFIED = -1,
    ASCREEN_ORIENTATION_LANDSCAPE = 0,
    ASCREEN_ORIENTATION_PORTRAIT = 1,
    ASCREEN_ORIENTATION_USER = 2,
    ASCREEN_ORIENTATION_BEHIND = 3,
    ASCREEN_ORIENTATION_SENSOR = 4,
    ASCREEN_ORIENTATION_NOSENSOR = 5,
    ASCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6,
    ASCREEN_ORIENTATION_SENSOR_PORTRAIT = 7,
    ASCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8,
    ASCREEN_ORIENTATION_REVERSE_PORTRAIT = 9,
    ASCREEN_ORIENTATION_FULL_SENSOR = 10,
};

enum {
    AEVENT_POWER_CONNECTED = 100,
    AEVENT_POWER_DISCONNECTED = 101,
    AEVENT_DOWNLOAD_COMPLETE = 110,
};

enum {
    ADOWNLOAD_NOT_SUPPORTED = -2,
    ADOWNLOAD_FAILED = -1,
    ADOWNLOAD_OK = 0,
    ADOWNLOAD_EXISTS = 1,
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

typedef uint8_t         jboolean;       /* unsigned 8 bits */
typedef int8_t          jbyte;          /* signed 8 bits */
typedef uint16_t        jchar;          /* unsigned 16 bits */
typedef int16_t         jshort;         /* signed 16 bits */
typedef int32_t         jint;           /* signed 32 bits */
typedef int64_t         jlong;          /* signed 64 bits */
typedef float           jfloat;         /* 32-bit IEEE 754 */
typedef double          jdouble;        /* 64-bit IEEE 754 */

typedef jint            jsize;

typedef void*           jobject;
typedef jobject         jclass;
typedef jobject         jstring;
typedef jobject         jarray;
typedef jarray          jobjectArray;
typedef jarray          jbooleanArray;
typedef jarray          jbyteArray;
typedef jarray          jcharArray;
typedef jarray          jshortArray;
typedef jarray          jintArray;
typedef jarray          jlongArray;
typedef jarray          jfloatArray;
typedef jarray          jdoubleArray;
typedef jobject         jthrowable;
typedef jobject         jweak;

struct _jfieldID;                       /* opaque structure */
typedef struct _jfieldID* jfieldID;     /* field IDs */

struct _jmethodID;                      /* opaque structure */
typedef struct _jmethodID* jmethodID;   /* method IDs */

struct JNIInvokeInterface;

typedef union jvalue {
    jboolean    z;
    jbyte       b;
    jchar       c;
    jshort      s;
    jint        i;
    jlong       j;
    jfloat      f;
    jdouble     d;
    jobject     l;
} jvalue;

typedef enum jobjectRefType {
    JNIInvalidRefType = 0,
    JNILocalRefType = 1,
    JNIGlobalRefType = 2,
    JNIWeakGlobalRefType = 3
} jobjectRefType;

typedef struct {
    const char* name;
    const char* signature;
    void*       fnPtr;
} JNINativeMethod;

struct _JNIEnv;
struct _JavaVM;
typedef const struct JNINativeInterface* C_JNIEnv;

typedef const struct JNINativeInterface* JNIEnv;
typedef const struct JNIInvokeInterface* JavaVM;

struct JNINativeInterface {
    void*       reserved0;
    void*       reserved1;
    void*       reserved2;
    void*       reserved3;

    jint        (*GetVersion)(JNIEnv *);

    jclass      (*DefineClass)(JNIEnv*, const char*, jobject, const jbyte*,
                        jsize);
    jclass      (*FindClass)(JNIEnv*, const char*);

    jmethodID   (*FromReflectedMethod)(JNIEnv*, jobject);
    jfieldID    (*FromReflectedField)(JNIEnv*, jobject);
    /* spec doesn't show jboolean parameter */
    jobject     (*ToReflectedMethod)(JNIEnv*, jclass, jmethodID, jboolean);

    jclass      (*GetSuperclass)(JNIEnv*, jclass);
    jboolean    (*IsAssignableFrom)(JNIEnv*, jclass, jclass);

    /* spec doesn't show jboolean parameter */
    jobject     (*ToReflectedField)(JNIEnv*, jclass, jfieldID, jboolean);

    jint        (*Throw)(JNIEnv*, jthrowable);
    jint        (*ThrowNew)(JNIEnv *, jclass, const char *);
    jthrowable  (*ExceptionOccurred)(JNIEnv*);
    void        (*ExceptionDescribe)(JNIEnv*);
    void        (*ExceptionClear)(JNIEnv*);
    void        (*FatalError)(JNIEnv*, const char*);

    jint        (*PushLocalFrame)(JNIEnv*, jint);
    jobject     (*PopLocalFrame)(JNIEnv*, jobject);

    jobject     (*NewGlobalRef)(JNIEnv*, jobject);
    void        (*DeleteGlobalRef)(JNIEnv*, jobject);
    void        (*DeleteLocalRef)(JNIEnv*, jobject);
    jboolean    (*IsSameObject)(JNIEnv*, jobject, jobject);

    jobject     (*NewLocalRef)(JNIEnv*, jobject);
    jint        (*EnsureLocalCapacity)(JNIEnv*, jint);

    jobject     (*AllocObject)(JNIEnv*, jclass);
    jobject     (*NewObject)(JNIEnv*, jclass, jmethodID, ...);
    jobject     (*NewObjectV)(JNIEnv*, jclass, jmethodID, va_list);
    jobject     (*NewObjectA)(JNIEnv*, jclass, jmethodID, jvalue*);

    jclass      (*GetObjectClass)(JNIEnv*, jobject);
    jboolean    (*IsInstanceOf)(JNIEnv*, jobject, jclass);
    jmethodID   (*GetMethodID)(JNIEnv*, jclass, const char*, const char*);

    jobject     (*CallObjectMethod)(JNIEnv*, jobject, jmethodID, ...);
    jobject     (*CallObjectMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jobject     (*CallObjectMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    jboolean    (*CallBooleanMethod)(JNIEnv*, jobject, jmethodID, ...);
    jboolean    (*CallBooleanMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jboolean    (*CallBooleanMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    jbyte       (*CallByteMethod)(JNIEnv*, jobject, jmethodID, ...);
    jbyte       (*CallByteMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jbyte       (*CallByteMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    jchar       (*CallCharMethod)(JNIEnv*, jobject, jmethodID, ...);
    jchar       (*CallCharMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jchar       (*CallCharMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    jshort      (*CallShortMethod)(JNIEnv*, jobject, jmethodID, ...);
    jshort      (*CallShortMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jshort      (*CallShortMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    jint        (*CallIntMethod)(JNIEnv*, jobject, jmethodID, ...);
    jint        (*CallIntMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jint        (*CallIntMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    jlong       (*CallLongMethod)(JNIEnv*, jobject, jmethodID, ...);
    jlong       (*CallLongMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jlong       (*CallLongMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    jfloat      (*CallFloatMethod)(JNIEnv*, jobject, jmethodID, ...);
    jfloat      (*CallFloatMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jfloat      (*CallFloatMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    jdouble     (*CallDoubleMethod)(JNIEnv*, jobject, jmethodID, ...);
    jdouble     (*CallDoubleMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    jdouble     (*CallDoubleMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);
    void        (*CallVoidMethod)(JNIEnv*, jobject, jmethodID, ...);
    void        (*CallVoidMethodV)(JNIEnv*, jobject, jmethodID, va_list);
    void        (*CallVoidMethodA)(JNIEnv*, jobject, jmethodID, jvalue*);

    jobject     (*CallNonvirtualObjectMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jobject     (*CallNonvirtualObjectMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    jobject     (*CallNonvirtualObjectMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);
    jboolean    (*CallNonvirtualBooleanMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jboolean    (*CallNonvirtualBooleanMethodV)(JNIEnv*, jobject, jclass,
                         jmethodID, va_list);
    jboolean    (*CallNonvirtualBooleanMethodA)(JNIEnv*, jobject, jclass,
                         jmethodID, jvalue*);
    jbyte       (*CallNonvirtualByteMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jbyte       (*CallNonvirtualByteMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    jbyte       (*CallNonvirtualByteMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);
    jchar       (*CallNonvirtualCharMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jchar       (*CallNonvirtualCharMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    jchar       (*CallNonvirtualCharMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);
    jshort      (*CallNonvirtualShortMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jshort      (*CallNonvirtualShortMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    jshort      (*CallNonvirtualShortMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);
    jint        (*CallNonvirtualIntMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jint        (*CallNonvirtualIntMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    jint        (*CallNonvirtualIntMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);
    jlong       (*CallNonvirtualLongMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jlong       (*CallNonvirtualLongMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    jlong       (*CallNonvirtualLongMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);
    jfloat      (*CallNonvirtualFloatMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jfloat      (*CallNonvirtualFloatMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    jfloat      (*CallNonvirtualFloatMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);
    jdouble     (*CallNonvirtualDoubleMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    jdouble     (*CallNonvirtualDoubleMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    jdouble     (*CallNonvirtualDoubleMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);
    void        (*CallNonvirtualVoidMethod)(JNIEnv*, jobject, jclass,
                        jmethodID, ...);
    void        (*CallNonvirtualVoidMethodV)(JNIEnv*, jobject, jclass,
                        jmethodID, va_list);
    void        (*CallNonvirtualVoidMethodA)(JNIEnv*, jobject, jclass,
                        jmethodID, jvalue*);

    jfieldID    (*GetFieldID)(JNIEnv*, jclass, const char*, const char*);

    jobject     (*GetObjectField)(JNIEnv*, jobject, jfieldID);
    jboolean    (*GetBooleanField)(JNIEnv*, jobject, jfieldID);
    jbyte       (*GetByteField)(JNIEnv*, jobject, jfieldID);
    jchar       (*GetCharField)(JNIEnv*, jobject, jfieldID);
    jshort      (*GetShortField)(JNIEnv*, jobject, jfieldID);
    jint        (*GetIntField)(JNIEnv*, jobject, jfieldID);
    jlong       (*GetLongField)(JNIEnv*, jobject, jfieldID);
    jfloat      (*GetFloatField)(JNIEnv*, jobject, jfieldID);
    jdouble     (*GetDoubleField)(JNIEnv*, jobject, jfieldID);

    void        (*SetObjectField)(JNIEnv*, jobject, jfieldID, jobject);
    void        (*SetBooleanField)(JNIEnv*, jobject, jfieldID, jboolean);
    void        (*SetByteField)(JNIEnv*, jobject, jfieldID, jbyte);
    void        (*SetCharField)(JNIEnv*, jobject, jfieldID, jchar);
    void        (*SetShortField)(JNIEnv*, jobject, jfieldID, jshort);
    void        (*SetIntField)(JNIEnv*, jobject, jfieldID, jint);
    void        (*SetLongField)(JNIEnv*, jobject, jfieldID, jlong);
    void        (*SetFloatField)(JNIEnv*, jobject, jfieldID, jfloat);
    void        (*SetDoubleField)(JNIEnv*, jobject, jfieldID, jdouble);

    jmethodID   (*GetStaticMethodID)(JNIEnv*, jclass, const char*, const char*);

    jobject     (*CallStaticObjectMethod)(JNIEnv*, jclass, jmethodID, ...);
    jobject     (*CallStaticObjectMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    jobject     (*CallStaticObjectMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);
    jboolean    (*CallStaticBooleanMethod)(JNIEnv*, jclass, jmethodID, ...);
    jboolean    (*CallStaticBooleanMethodV)(JNIEnv*, jclass, jmethodID,
                        va_list);
    jboolean    (*CallStaticBooleanMethodA)(JNIEnv*, jclass, jmethodID,
                        jvalue*);
    jbyte       (*CallStaticByteMethod)(JNIEnv*, jclass, jmethodID, ...);
    jbyte       (*CallStaticByteMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    jbyte       (*CallStaticByteMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);
    jchar       (*CallStaticCharMethod)(JNIEnv*, jclass, jmethodID, ...);
    jchar       (*CallStaticCharMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    jchar       (*CallStaticCharMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);
    jshort      (*CallStaticShortMethod)(JNIEnv*, jclass, jmethodID, ...);
    jshort      (*CallStaticShortMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    jshort      (*CallStaticShortMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);
    jint        (*CallStaticIntMethod)(JNIEnv*, jclass, jmethodID, ...);
    jint        (*CallStaticIntMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    jint        (*CallStaticIntMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);
    jlong       (*CallStaticLongMethod)(JNIEnv*, jclass, jmethodID, ...);
    jlong       (*CallStaticLongMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    jlong       (*CallStaticLongMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);
    jfloat      (*CallStaticFloatMethod)(JNIEnv*, jclass, jmethodID, ...);
    jfloat      (*CallStaticFloatMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    jfloat      (*CallStaticFloatMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);
    jdouble     (*CallStaticDoubleMethod)(JNIEnv*, jclass, jmethodID, ...);
    jdouble     (*CallStaticDoubleMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    jdouble     (*CallStaticDoubleMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);
    void        (*CallStaticVoidMethod)(JNIEnv*, jclass, jmethodID, ...);
    void        (*CallStaticVoidMethodV)(JNIEnv*, jclass, jmethodID, va_list);
    void        (*CallStaticVoidMethodA)(JNIEnv*, jclass, jmethodID, jvalue*);

    jfieldID    (*GetStaticFieldID)(JNIEnv*, jclass, const char*,
                        const char*);

    jobject     (*GetStaticObjectField)(JNIEnv*, jclass, jfieldID);
    jboolean    (*GetStaticBooleanField)(JNIEnv*, jclass, jfieldID);
    jbyte       (*GetStaticByteField)(JNIEnv*, jclass, jfieldID);
    jchar       (*GetStaticCharField)(JNIEnv*, jclass, jfieldID);
    jshort      (*GetStaticShortField)(JNIEnv*, jclass, jfieldID);
    jint        (*GetStaticIntField)(JNIEnv*, jclass, jfieldID);
    jlong       (*GetStaticLongField)(JNIEnv*, jclass, jfieldID);
    jfloat      (*GetStaticFloatField)(JNIEnv*, jclass, jfieldID);
    jdouble     (*GetStaticDoubleField)(JNIEnv*, jclass, jfieldID);

    void        (*SetStaticObjectField)(JNIEnv*, jclass, jfieldID, jobject);
    void        (*SetStaticBooleanField)(JNIEnv*, jclass, jfieldID, jboolean);
    void        (*SetStaticByteField)(JNIEnv*, jclass, jfieldID, jbyte);
    void        (*SetStaticCharField)(JNIEnv*, jclass, jfieldID, jchar);
    void        (*SetStaticShortField)(JNIEnv*, jclass, jfieldID, jshort);
    void        (*SetStaticIntField)(JNIEnv*, jclass, jfieldID, jint);
    void        (*SetStaticLongField)(JNIEnv*, jclass, jfieldID, jlong);
    void        (*SetStaticFloatField)(JNIEnv*, jclass, jfieldID, jfloat);
    void        (*SetStaticDoubleField)(JNIEnv*, jclass, jfieldID, jdouble);

    jstring     (*NewString)(JNIEnv*, const jchar*, jsize);
    jsize       (*GetStringLength)(JNIEnv*, jstring);
    const jchar* (*GetStringChars)(JNIEnv*, jstring, jboolean*);
    void        (*ReleaseStringChars)(JNIEnv*, jstring, const jchar*);
    jstring     (*NewStringUTF)(JNIEnv*, const char*);
    jsize       (*GetStringUTFLength)(JNIEnv*, jstring);
    /* JNI spec says this returns const jbyte*, but that's inconsistent */
    const char* (*GetStringUTFChars)(JNIEnv*, jstring, jboolean*);
    void        (*ReleaseStringUTFChars)(JNIEnv*, jstring, const char*);
    jsize       (*GetArrayLength)(JNIEnv*, jarray);
    jobjectArray (*NewObjectArray)(JNIEnv*, jsize, jclass, jobject);
    jobject     (*GetObjectArrayElement)(JNIEnv*, jobjectArray, jsize);
    void        (*SetObjectArrayElement)(JNIEnv*, jobjectArray, jsize, jobject);

    jbooleanArray (*NewBooleanArray)(JNIEnv*, jsize);
    jbyteArray    (*NewByteArray)(JNIEnv*, jsize);
    jcharArray    (*NewCharArray)(JNIEnv*, jsize);
    jshortArray   (*NewShortArray)(JNIEnv*, jsize);
    jintArray     (*NewIntArray)(JNIEnv*, jsize);
    jlongArray    (*NewLongArray)(JNIEnv*, jsize);
    jfloatArray   (*NewFloatArray)(JNIEnv*, jsize);
    jdoubleArray  (*NewDoubleArray)(JNIEnv*, jsize);

    jboolean*   (*GetBooleanArrayElements)(JNIEnv*, jbooleanArray, jboolean*);
    jbyte*      (*GetByteArrayElements)(JNIEnv*, jbyteArray, jboolean*);
    jchar*      (*GetCharArrayElements)(JNIEnv*, jcharArray, jboolean*);
    jshort*     (*GetShortArrayElements)(JNIEnv*, jshortArray, jboolean*);
    jint*       (*GetIntArrayElements)(JNIEnv*, jintArray, jboolean*);
    jlong*      (*GetLongArrayElements)(JNIEnv*, jlongArray, jboolean*);
    jfloat*     (*GetFloatArrayElements)(JNIEnv*, jfloatArray, jboolean*);
    jdouble*    (*GetDoubleArrayElements)(JNIEnv*, jdoubleArray, jboolean*);

    void        (*ReleaseBooleanArrayElements)(JNIEnv*, jbooleanArray,
                        jboolean*, jint);
    void        (*ReleaseByteArrayElements)(JNIEnv*, jbyteArray,
                        jbyte*, jint);
    void        (*ReleaseCharArrayElements)(JNIEnv*, jcharArray,
                        jchar*, jint);
    void        (*ReleaseShortArrayElements)(JNIEnv*, jshortArray,
                        jshort*, jint);
    void        (*ReleaseIntArrayElements)(JNIEnv*, jintArray,
                        jint*, jint);
    void        (*ReleaseLongArrayElements)(JNIEnv*, jlongArray,
                        jlong*, jint);
    void        (*ReleaseFloatArrayElements)(JNIEnv*, jfloatArray,
                        jfloat*, jint);
    void        (*ReleaseDoubleArrayElements)(JNIEnv*, jdoubleArray,
                        jdouble*, jint);

    void        (*GetBooleanArrayRegion)(JNIEnv*, jbooleanArray,
                        jsize, jsize, jboolean*);
    void        (*GetByteArrayRegion)(JNIEnv*, jbyteArray,
                        jsize, jsize, jbyte*);
    void        (*GetCharArrayRegion)(JNIEnv*, jcharArray,
                        jsize, jsize, jchar*);
    void        (*GetShortArrayRegion)(JNIEnv*, jshortArray,
                        jsize, jsize, jshort*);
    void        (*GetIntArrayRegion)(JNIEnv*, jintArray,
                        jsize, jsize, jint*);
    void        (*GetLongArrayRegion)(JNIEnv*, jlongArray,
                        jsize, jsize, jlong*);
    void        (*GetFloatArrayRegion)(JNIEnv*, jfloatArray,
                        jsize, jsize, jfloat*);
    void        (*GetDoubleArrayRegion)(JNIEnv*, jdoubleArray,
                        jsize, jsize, jdouble*);

    /* spec shows these without const; some jni.h do, some don't */
    void        (*SetBooleanArrayRegion)(JNIEnv*, jbooleanArray,
                        jsize, jsize, const jboolean*);
    void        (*SetByteArrayRegion)(JNIEnv*, jbyteArray,
                        jsize, jsize, const jbyte*);
    void        (*SetCharArrayRegion)(JNIEnv*, jcharArray,
                        jsize, jsize, const jchar*);
    void        (*SetShortArrayRegion)(JNIEnv*, jshortArray,
                        jsize, jsize, const jshort*);
    void        (*SetIntArrayRegion)(JNIEnv*, jintArray,
                        jsize, jsize, const jint*);
    void        (*SetLongArrayRegion)(JNIEnv*, jlongArray,
                        jsize, jsize, const jlong*);
    void        (*SetFloatArrayRegion)(JNIEnv*, jfloatArray,
                        jsize, jsize, const jfloat*);
    void        (*SetDoubleArrayRegion)(JNIEnv*, jdoubleArray,
                        jsize, jsize, const jdouble*);

    jint        (*RegisterNatives)(JNIEnv*, jclass, const JNINativeMethod*,
                        jint);
    jint        (*UnregisterNatives)(JNIEnv*, jclass);
    jint        (*MonitorEnter)(JNIEnv*, jobject);
    jint        (*MonitorExit)(JNIEnv*, jobject);
    jint        (*GetJavaVM)(JNIEnv*, JavaVM**);

    void        (*GetStringRegion)(JNIEnv*, jstring, jsize, jsize, jchar*);
    void        (*GetStringUTFRegion)(JNIEnv*, jstring, jsize, jsize, char*);

    void*       (*GetPrimitiveArrayCritical)(JNIEnv*, jarray, jboolean*);
    void        (*ReleasePrimitiveArrayCritical)(JNIEnv*, jarray, void*, jint);

    const jchar* (*GetStringCritical)(JNIEnv*, jstring, jboolean*);
    void        (*ReleaseStringCritical)(JNIEnv*, jstring, const jchar*);

    jweak       (*NewWeakGlobalRef)(JNIEnv*, jobject);
    void        (*DeleteWeakGlobalRef)(JNIEnv*, jweak);

    jboolean    (*ExceptionCheck)(JNIEnv*);

    jobject     (*NewDirectByteBuffer)(JNIEnv*, void*, jlong);
    void*       (*GetDirectBufferAddress)(JNIEnv*, jobject);
    jlong       (*GetDirectBufferCapacity)(JNIEnv*, jobject);

    /* added in JNI 1.6 */
    jobjectRefType (*GetObjectRefType)(JNIEnv*, jobject);
};

struct JNIInvokeInterface {
    void*       reserved0;
    void*       reserved1;
    void*       reserved2;

    jint        (*DestroyJavaVM)(JavaVM*);
    jint        (*AttachCurrentThread)(JavaVM*, JNIEnv**, void*);
    jint        (*DetachCurrentThread)(JavaVM*);
    jint        (*GetEnv)(JavaVM*, void**, jint);
    jint        (*AttachCurrentThreadAsDaemon)(JavaVM*, JNIEnv**, void*);
};

struct _JavaVM {
    const struct JNIInvokeInterface* functions;
};

struct JavaVMAttachArgs {
    jint        version;    /* must be >= JNI_VERSION_1_2 */
    const char* name;       /* NULL or name of thread as modified UTF-8 str */
    jobject     group;      /* global ref of a ThreadGroup object, or NULL */
};
typedef struct JavaVMAttachArgs JavaVMAttachArgs;

/*
 * JNI 1.2+ initialization.  (As of 1.6, the pre-1.2 structures are no
 * longer supported.)
 */
typedef struct JavaVMOption {
    const char* optionString;
    void*       extraInfo;
} JavaVMOption;

typedef struct JavaVMInitArgs {
    jint        version;    /* use JNI_VERSION_1_2 or later */

    jint        nOptions;
    JavaVMOption* options;
    jboolean    ignoreUnrecognized;
} JavaVMInitArgs;

static const int JNI_FALSE = 0;
static const int JNI_TRUE = 1;

static const int JNI_VERSION_1_1 = 0x00010001;
static const int JNI_VERSION_1_2 = 0x00010002;
static const int JNI_VERSION_1_4 = 0x00010004;
static const int JNI_VERSION_1_6 = 0x00010006;

static const int JNI_OK        = (0);         /* no error */
static const int JNI_ERR       = (-1);        /* generic error */
static const int JNI_EDETACHED = (-2);        /* thread detached from the VM */
static const int JNI_EVERSION  = (-3);        /* JNI version error */

static const int JNI_COMMIT    = 1;           /* copy content, do not free buffer */
static const int JNI_ABORT     = 2;           /* free buffer w/o copying back */


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

void ANativeActivity_finish(ANativeActivity* activity);
void ANativeActivity_setWindowFormat(ANativeActivity* activity, int32_t format);
void ANativeActivity_setWindowFlags(ANativeActivity* activity,
        uint32_t addFlags, uint32_t removeFlags);
enum {
    ANATIVEACTIVITY_SHOW_SOFT_INPUT_IMPLICIT = 0x0001,
    ANATIVEACTIVITY_SHOW_SOFT_INPUT_FORCED = 0x0002,
};
void ANativeActivity_showSoftInput(ANativeActivity* activity, uint32_t flags);
enum {
    ANATIVEACTIVITY_HIDE_SOFT_INPUT_IMPLICIT_ONLY = 0x0001,
    ANATIVEACTIVITY_HIDE_SOFT_INPUT_NOT_ALWAYS = 0x0002,
};
void ANativeActivity_hideSoftInput(ANativeActivity* activity, uint32_t flags);


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
    // 0 - 4
    APP_CMD_INPUT_CHANGED,
    APP_CMD_INIT_WINDOW,
    APP_CMD_TERM_WINDOW,
    APP_CMD_WINDOW_RESIZED,
    APP_CMD_WINDOW_REDRAW_NEEDED,
    // 5 - 9
    APP_CMD_CONTENT_RECT_CHANGED,
    APP_CMD_GAINED_FOCUS,
    APP_CMD_LOST_FOCUS,
    APP_CMD_CONFIG_CHANGED,
    APP_CMD_LOW_MEMORY,
    // 10 - 14
    APP_CMD_START,
    APP_CMD_RESUME,
    APP_CMD_SAVE_STATE,
    APP_CMD_PAUSE,
    APP_CMD_STOP,
    // 15
    APP_CMD_DESTROY,
};

int8_t android_app_read_cmd(struct android_app* android_app);
void android_app_pre_exec_cmd(struct android_app* android_app, int8_t cmd);
void android_app_post_exec_cmd(struct android_app* android_app, int8_t cmd);

// from android-ndk/platforms/android-9/arch-arm/usr/include/configuration.h:
struct AConfiguration;
typedef struct AConfiguration AConfiguration;

enum {
    ACONFIGURATION_ORIENTATION_ANY  = 0x0000,
    ACONFIGURATION_ORIENTATION_PORT = 0x0001,
    ACONFIGURATION_ORIENTATION_LAND = 0x0002,
    ACONFIGURATION_ORIENTATION_SQUARE = 0x0003,

    ACONFIGURATION_TOUCHSCREEN_ANY  = 0x0000,
    ACONFIGURATION_TOUCHSCREEN_NOTOUCH  = 0x0001,
    ACONFIGURATION_TOUCHSCREEN_STYLUS  = 0x0002,
    ACONFIGURATION_TOUCHSCREEN_FINGER  = 0x0003,

    ACONFIGURATION_DENSITY_DEFAULT = 0,
    ACONFIGURATION_DENSITY_LOW = 120,
    ACONFIGURATION_DENSITY_MEDIUM = 160,
    ACONFIGURATION_DENSITY_HIGH = 240,
    ACONFIGURATION_DENSITY_NONE = 0xffff,

    ACONFIGURATION_KEYBOARD_ANY  = 0x0000,
    ACONFIGURATION_KEYBOARD_NOKEYS  = 0x0001,
    ACONFIGURATION_KEYBOARD_QWERTY  = 0x0002,
    ACONFIGURATION_KEYBOARD_12KEY  = 0x0003,

    ACONFIGURATION_NAVIGATION_ANY  = 0x0000,
    ACONFIGURATION_NAVIGATION_NONAV  = 0x0001,
    ACONFIGURATION_NAVIGATION_DPAD  = 0x0002,
    ACONFIGURATION_NAVIGATION_TRACKBALL  = 0x0003,
    ACONFIGURATION_NAVIGATION_WHEEL  = 0x0004,

    ACONFIGURATION_KEYSHIDDEN_ANY = 0x0000,
    ACONFIGURATION_KEYSHIDDEN_NO = 0x0001,
    ACONFIGURATION_KEYSHIDDEN_YES = 0x0002,
    ACONFIGURATION_KEYSHIDDEN_SOFT = 0x0003,

    ACONFIGURATION_NAVHIDDEN_ANY = 0x0000,
    ACONFIGURATION_NAVHIDDEN_NO = 0x0001,
    ACONFIGURATION_NAVHIDDEN_YES = 0x0002,

    ACONFIGURATION_SCREENSIZE_ANY  = 0x00,
    ACONFIGURATION_SCREENSIZE_SMALL = 0x01,
    ACONFIGURATION_SCREENSIZE_NORMAL = 0x02,
    ACONFIGURATION_SCREENSIZE_LARGE = 0x03,
    ACONFIGURATION_SCREENSIZE_XLARGE = 0x04,

    ACONFIGURATION_SCREENLONG_ANY = 0x00,
    ACONFIGURATION_SCREENLONG_NO = 0x1,
    ACONFIGURATION_SCREENLONG_YES = 0x2,

    ACONFIGURATION_UI_MODE_TYPE_ANY = 0x00,
    ACONFIGURATION_UI_MODE_TYPE_NORMAL = 0x01,
    ACONFIGURATION_UI_MODE_TYPE_DESK = 0x02,
    ACONFIGURATION_UI_MODE_TYPE_CAR = 0x03,

    ACONFIGURATION_UI_MODE_NIGHT_ANY = 0x00,
    ACONFIGURATION_UI_MODE_NIGHT_NO = 0x1,
    ACONFIGURATION_UI_MODE_NIGHT_YES = 0x2,

    ACONFIGURATION_MCC = 0x0001,
    ACONFIGURATION_MNC = 0x0002,
    ACONFIGURATION_LOCALE = 0x0004,
    ACONFIGURATION_TOUCHSCREEN = 0x0008,
    ACONFIGURATION_KEYBOARD = 0x0010,
    ACONFIGURATION_KEYBOARD_HIDDEN = 0x0020,
    ACONFIGURATION_NAVIGATION = 0x0040,
    ACONFIGURATION_ORIENTATION = 0x0080,
    ACONFIGURATION_DENSITY = 0x0100,
    ACONFIGURATION_SCREEN_SIZE = 0x0200,
    ACONFIGURATION_VERSION = 0x0400,
    ACONFIGURATION_SCREEN_LAYOUT = 0x0800,
    ACONFIGURATION_UI_MODE = 0x1000,
};

int32_t AConfiguration_getDensity(AConfiguration* config);
int32_t AConfiguration_getKeyboard(AConfiguration* config);
int32_t AConfiguration_getTouchscreen(AConfiguration* config);
int32_t AConfiguration_getScreenSize(AConfiguration* config);
void AConfiguration_getLanguage(AConfiguration* config, char* outLanguage);
void AConfiguration_getCountry(AConfiguration* config, char* outCountry);
]]

-- JNI Interfacing
local C = ffi.C

local JNI = {}

function JNI:context(jvm, runnable)
    self.jvm = jvm

    local env = ffi.new("JNIEnv*[1]")
    self.jvm[0].GetEnv(self.jvm, ffi.cast("void**", env), C.JNI_VERSION_1_6)

    assert(self.jvm[0].AttachCurrentThread(self.jvm, env, nil) ~= C.JNI_ERR,
        "cannot attach JVM to current thread")

    self.env = env[0]
    local result = { runnable(self) }

    self.jvm[0].DetachCurrentThread(self.jvm)
    self.env = nil

    return unpack(result)
end

function JNI:callVoidMethod(object, method, signature, ...)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local methodID = self.env[0].GetMethodID(self.env, clazz, method, signature)
    self.env[0].CallVoidMethod(self.env, object, methodID, ...)
    self.env[0].DeleteLocalRef(self.env, clazz)
end

function JNI:callStaticVoidMethod(class, method, signature, ...)
    local clazz = self.env[0].FindClass(self.env, class)
    local methodID = self.env[0].GetStaticMethodID(self.env, clazz, method, signature)
    self.env[0].callStaticVoidMethod(self.env, clazz, methodID, ...)
    self.env[0].DeleteLocalRef(self.env, clazz)
end

function JNI:callIntMethod(object, method, signature, ...)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local methodID = self.env[0].GetMethodID(self.env, clazz, method, signature)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return self.env[0].CallIntMethod(self.env, object, methodID, ...)
end

function JNI:callStaticIntMethod(class, method, signature, ...)
    local clazz = self.env[0].FindClass(self.env, class)
    local methodID = self.env[0].GetStaticMethodID(self.env, clazz, method, signature)
    local res = self.env[0].CallStaticIntMethod(self.env, clazz, methodID, ...)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return res
end

function JNI:callBooleanMethod(object, method, signature, ...)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local methodID = self.env[0].GetMethodID(self.env, clazz, method, signature)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return self.env[0].CallBooleanMethod(self.env, object, methodID, ...) == C.JNI_TRUE
end

function JNI:callStaticBooleanMethod(class, method, signature, ...)
    local clazz = self.env[0].FindClass(self.env, class)
    local methodID = self.env[0].GetStaticMethodID(self.env, clazz, method, signature)
    local res = self.env[0].CallStaticBooleanMethod(self.env, clazz, methodID, ...)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return res == C.JNI_TRUE
end

function JNI:callObjectMethod(object, method, signature, ...)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local methodID = self.env[0].GetMethodID(self.env, clazz, method, signature)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return self.env[0].CallObjectMethod(self.env, object, methodID, ...)
end

function JNI:callStaticObjectMethod(class, method, signature, ...)
    local clazz = self.env[0].FindClass(self.env, class)
    local methodID = self.env[0].GetStaticMethodID(self.env, clazz, method, signature)
    local res = self.env[0].CallStaticObjectMethod(self.env, clazz, methodID, ...)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return res
end

function JNI:getObjectField(object, field, signature)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local fieldID = self.env[0].GetFieldID(self.env, clazz, field, signature)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return self.env[0].GetObjectField(self.env, object, fieldID)
end

function JNI:setObjectField(object, field, signature, value)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local fieldID = self.env[0].GetFieldID(self.env, clazz, field, signature)
    self.env[0].SetObjectField(self.env, object, fieldID, value)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return object
end

function JNI:setFloatField(object, field, signature, value)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local fieldID = self.env[0].GetFieldID(self.env, clazz, field, signature)
    self.env[0].SetFloatField(self.env, object, fieldID, value)
    self.env[0].DeleteLocalRef(self.env, clazz)
    return object
end

function JNI:to_string(javastring)
    local utf = self.env[0].GetStringUTFChars(self.env, javastring, nil)
    local luastr = ffi.string(utf, self.env[0].GetStringUTFLength(self.env, javastring))
    self.env[0].ReleaseStringUTFChars(self.env, javastring, utf)
    return luastr
end

-- Android specific

-- We need to load libandroid, liblog, and the app glue: they're no longer in the global namespace
-- as we're now running under a plain LuaJIT.
-- NOTE: We haven't overloaded ffi.load yet
--       (and we can't, as our custom dlopen wrapper depends on libandroid and liblog for logging ^^),
--       so, we hope that the fact we've kept linking libluajit-launcher against libandroid and liblog will be enough
--       to satisfy old and broken platforms where dlopen and/or loading DT_NEEDED libraries is extra finicky...
local android_lib_ok, android_lib = pcall(ffi.load, "libandroid.so")
local android_log_ok, android_log = pcall(ffi.load, "liblog.so")
local android_glue_ok, android_glue = pcall(ffi.load, "libluajit-launcher.so")
local android = {
    app = nil,
    jni = JNI,
    log_name = "luajit-launcher",
    lib = android_lib_ok and android_lib or C,
    log = android_log_ok and android_log or C,
    glue = android_glue_ok and android_glue or C,
}

function android.LOG(level, message)
    android.log.__android_log_print(level, android.log_name, "%s", message)
end

function android.LOGVV(tag, message)
    android.log.__android_log_print(C.ANDROID_LOG_VERBOSE, tag, "%s", message)
end

function android.LOGV(message)
    android.LOG(C.ANDROID_LOG_VERBOSE, message)
end
function android.LOGD(message)
    android.LOG(C.ANDROID_LOG_DEBUG, message)
end
function android.LOGI(message)
    android.LOG(C.ANDROID_LOG_INFO, message)
end
function android.LOGW(message)
    android.LOG(C.ANDROID_LOG_WARN, message)
end
function android.LOGE(message)
    android.LOG(C.ANDROID_LOG_ERROR, message)
end

--[[--
A loader function for Lua which will look for assets when loading modules.

@string modulename
@return module or error message
--]]
function android.asset_loader(modulename)
    local errmsg = ""
    -- Find source
    local modulepath = string.gsub(modulename, "%.", "/")
    local filename = string.gsub("?.lua", "%?", modulepath)
    local asset = android.lib.AAssetManager_open(
        android.app.activity.assetManager,
        filename, C.AASSET_MODE_BUFFER)
    --android.LOGI(string.format("trying to open asset %s: %s", filename, tostring(asset)))
    if asset ~= nil then
        -- read asset:
        local assetdata = android.lib.AAsset_getBuffer(asset)
        local assetsize = android.lib.AAsset_getLength(asset)
        if assetdata ~= nil then
            -- Compile and return the module
            local compiled = assert(loadstring(ffi.string(assetdata, assetsize), filename))
            android.lib.AAsset_close(asset)
            return compiled
        else
            android.lib.AAsset_close(asset)
            errmsg = errmsg.."\n\tunaccessible file '"..filename.."' (tried with asset loader)"
        end
    else
        errmsg = errmsg.."\n\tno file '"..filename.."' (checked with asset loader)"
    end
    return errmsg
end

--[[--
This loader function just loads dependency libraries for the C module.

@string modulename
@treturn bool success or failure
--]]
function android.deplib_loader(modulename)
    local function readable(filename)
        local f = io.open(filename, "r")
        if f == nil then return false end
        f:close()
        return true
    end

    local modulepath = string.gsub(modulename, "%.", "/")
    for path in string.gmatch(package.cpath, "([^;]+)") do
        local module = string.gsub(path, "%?", modulepath)
        -- try to load dependencies of this module with our dlopen implementation
        if readable(module) then
            android.DEBUG("try to load module "..module)
            local ok, err = pcall(android.dl.dlopen, module)
            if ok then return end
            if err then
                android.LOGE("error: " .. err)
            end
        end
    end
end

function android.get_application_directory()
end

--[[--
The C code will call this function.
--]]
local function run(android_app_state)
    android.app = ffi.cast("struct android_app*", android_app_state)

    android.dir, android.nativeLibraryDir =
        JNI:context(android.app.activity.vm, function(jni)
            local files_dir = jni:callObjectMethod(
                jni:callObjectMethod(
                    android.app.activity.clazz,
                    "getFilesDir",
                    "()Ljava/io/File;"
                ),
                "getAbsolutePath",
                "()Ljava/lang/String;"
            )
            local app_info = jni:getObjectField(
                jni:callObjectMethod(
                    jni:callObjectMethod(
                        android.app.activity.clazz,
                        "getPackageManager",
                        "()Landroid/content/pm/PackageManager;"
                    ),
                    "getPackageInfo",
                    "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;",
                    jni:callObjectMethod(
                        android.app.activity.clazz,
                        "getPackageName",
                        "()Ljava/lang/String;"
                    ),
                    0
                ),
                "applicationInfo",
                "Landroid/content/pm/ApplicationInfo;"
            )
            return
                jni:to_string(files_dir),
                jni:to_string(jni:getObjectField(app_info, "nativeLibraryDir", "Ljava/lang/String;"))
        end)

    android.extractAssets = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "extractAssets",
                "()Z"
            )
        end)
    end

    android.getIntent = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local path = jni:callObjectMethod(
                android.app.activity.clazz,
                "getFilePathFromIntent",
                "()Ljava/lang/String;"
            )
            if path ~= nil then
                return jni:to_string(path)
            end
        end)
    end

    android.getExternalStoragePath = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local external_path = jni:callObjectMethod(
                android.app.activity.clazz,
                "getExternalPath",
                "()Ljava/lang/String;"
            )
            return jni:to_string(external_path)
        end)
    end

    android.getExternalSdPath = function()
        return JNI:context(android.app.activity.vm, function(jni)
                local ext_sd_path = jni:callObjectMethod(
                    android.app.activity.clazz,
                    "getExternalSdPath",
                    "()Ljava/lang/String;"
                )

                local res = jni:to_string(ext_sd_path)

                if res == "null" then
                    return false
                else
                    return true, res
                end
            end)
        end

    android.importFile = function(path)
        return JNI:context(android.app.activity.vm, function(jni)
            local import_path = jni.env[0].NewStringUTF(jni.env, path)
            local ok = jni:callBooleanMethod(
                android.app.activity.clazz,
                "safFilePicker",
                "(Ljava/lang/String;)Z",
                import_path
            )
            jni.env[0].DeleteLocalRef(jni.env, import_path)
            return ok
        end)
    end

    android.getLastImportedPath = function()
        return JNI:context(android.app.activity.vm, function(jni)
            -- get last imported file path
            local path = jni:callObjectMethod(
                android.app.activity.clazz,
                "getLastImportedPath",
                "()Ljava/lang/String;"
            )
            if path ~= nil then
                return jni:to_string(path)
            end
        end)
    end

    android.isPathInsideSandbox = function(path)
        if not path then return end
        return JNI:context(android.app.activity.vm, function(jni)
            local import_path = jni.env[0].NewStringUTF(jni.env, path)
            local ok = jni:callBooleanMethod(
                android.app.activity.clazz,
                "isPathInsideSandbox",
                "(Ljava/lang/String;)Z",
                import_path
            )
            jni.env[0].DeleteLocalRef(jni.env, import_path)
            return ok
        end)
    end

    --- Device identification.
    -- @treturn string product
    android.getProduct = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local product = jni:callObjectMethod(
                android.app.activity.clazz,
                "getProduct",
                "()Ljava/lang/String;"
            )
            return jni:to_string(product) or "unknown"
        end)
    end

    android.getVersion =  function()
        return JNI:context(android.app.activity.vm, function(jni)
            local version = jni:callObjectMethod(
                android.app.activity.clazz,
                "getVersion",
                "()Ljava/lang/String;"
            )
            return jni:to_string(version) or ""
        end)
    end

    --- Build identification.
    -- @treturn string flavor
    android.getFlavor = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local flavor = jni:callObjectMethod(
                android.app.activity.clazz,
                "getFlavor",
                "()Ljava/lang/String;"
            )
            return jni:to_string(flavor)
        end)
    end

    android.getName = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local name = jni:callObjectMethod(
                android.app.activity.clazz,
                "getName",
                "()Ljava/lang/String;"
            )
            return jni:to_string(name)
        end)
    end

    android.isDebuggable = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "isDebuggable",
                "()Z"
            )
        end)
    end

    android.supportsRuntimeChanges = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "hasRuntimeChanges",
                "()Z"
            )
        end)
    end

    -- input settings
    android.input = {}

    -- ignore some events
    android.input.ignore_touchscreen = false
    android.input.ignore_volume_keys = false
    android.input.ignore_back_button = false

    android.getVolumeKeysIgnored = function()
        return android.input.ignore_volume_keys
    end

    android.setVolumeKeysIgnored = function(ignored)
        android.input.ignore_volume_keys = ignored
    end

    android.isBackButtonIgnored = function()
        return android.input.ignore_back_button
    end

    android.setBackButtonIgnored = function(ignored)
        android.input.ignore_back_button = ignored
    end

    android.isTouchscreenIgnored = function()
        return android.input.ignore_touchscreen
    end

    android.toggleTouchscreenIgnored = function()
        android.input.ignore_touchscreen = not android.input.ignore_touchscreen
    end

    -- timeout settings
    android.timeout = {}
    android.timeout.app = 0
    android.timeout.get = function() return android.timeout.app end
    android.timeout.set = function(timeout)
        if type(timeout) == "number" then
            android.timeout.app = timeout
            JNI:context(android.app.activity.vm, function(jni)
                jni:callVoidMethod(
                    android.app.activity.clazz,
                    "setScreenOffTimeout",
                    "(I)V",
                    ffi.new('int32_t', timeout)
                )
            end)
        end
    end

    -- light settings
    android.lights = {}

    android.lights.dialogState = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getLightDialogState",
                "()I"
            )
        end)
    end

    android.lights.showDialog = function(title, intensity, warmth, okButton, cancelButton)
        JNI:context(android.app.activity.vm, function(jni)
            local t = jni.env[0].NewStringUTF(jni.env, title)
            local i = jni.env[0].NewStringUTF(jni.env, intensity)
            local w = jni.env[0].NewStringUTF(jni.env, warmth)
            local o = jni.env[0].NewStringUTF(jni.env, okButton)
            local c = jni.env[0].NewStringUTF(jni.env, cancelButton)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "showFrontlightDialog",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                t, i, w, o, c
            )
            jni.env[0].DeleteLocalRef(jni.env, t)
            jni.env[0].DeleteLocalRef(jni.env, i)
            jni.env[0].DeleteLocalRef(jni.env, w)
            jni.env[0].DeleteLocalRef(jni.env, o)
            jni.env[0].DeleteLocalRef(jni.env, c)
        end)
    end

    android.settings = {
        hasPermission = function(permission)
            if type(permission) ~= "string" then return end
            if permission == "battery" then
                permission = "canIgnoreBatteryOptimizations"
            elseif permission == "settings" then
                permission = "canWriteSystemSettings"
            else
                return false
            end
            return JNI:context(android.app.activity.vm, function(jni)
                return jni:callBooleanMethod(
                    android.app.activity.clazz,
                    permission,
                    "()Z"
                )
            end)
        end,
        requestPermission = function(permission, rationale, okButton, cancelButton)
            if type(permission) ~= "string" then return end
            if permission == "battery" then
                permission = "requestIgnoreBatteryOptimizations"
            elseif permission == "settings" then
                permission = "requestWriteSystemSettings"
            else
                permission = nil
            end
            if permission and rationale and okButton and cancelButton then
                JNI:context(android.app.activity.vm, function(jni)
                    local t = jni.env[0].NewStringUTF(jni.env, rationale)
                    local o = jni.env[0].NewStringUTF(jni.env, okButton)
                    local c = jni.env[0].NewStringUTF(jni.env, cancelButton)
                    jni:callVoidMethod(
                        android.app.activity.clazz,
                        permission,
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                        t, o, c
                    )
                    jni.env[0].DeleteLocalRef(jni.env, t)
                    jni.env[0].DeleteLocalRef(jni.env, o)
                    jni.env[0].DeleteLocalRef(jni.env, c)
                end)
            end
        end,
    }

    android.hapticOverride = false

    android.hapticFeedback = function(type)
        local force = android.hapticOverride and 1 or 0
        JNI:context(android.app.activity.vm, function(jni)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "performHapticFeedback",
                "(II)V",
                ffi.new("int32_t", type),
                ffi.new("int32_t", force)
            )
        end)
    end

    android.setHapticOverride = function(enable)
        android.hapticOverride = enable or false
    end

    android.setIgnoreInput = function(enable)
        JNI:context(android.app.activity.vm, function(jni)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "setIgnoreInput",
                "(Z)V",
                ffi.new("bool", enable)
            )
        end)
    end

    -- properties that don't change during the execution of the program.
    android.prop = {}

    -- build properties
    android.prop.name = android.getName()
    android.prop.flavor = android.getFlavor()
    android.prop.isDebuggable = android.isDebuggable()
    android.prop.runtimeChanges = android.supportsRuntimeChanges()

    -- device properties
    android.prop.product = android.getProduct()
    android.prop.version = android.getVersion()

    -- update logger name
    android.log_name = android.prop.name

    -- debug logs only if the build is debuggable
    android.DEBUG = function(message)
        if android.prop.isDebuggable then
            android.LOGD(message)
        end
    end

    --- Gets screen width.
    -- @treturn int screen width
    android.getScreenWidth = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenWidth",
                "()I"
            )
        end)
    end

    --- Gets screen height.
    -- @treturn int screen height
    android.getScreenHeight = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenHeight",
                "()I"
            )
        end)
    end

    android.getScreenAvailableWidth = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenAvailableWidth",
                "()I"
            )
        end)
    end

    android.getScreenAvailableHeight = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenAvailableHeight",
                "()I"
            )
        end)
    end

    android.screen = {}
    android.screen.width = android.getScreenWidth()
    android.screen.height = android.getScreenHeight()

    android.ota = {
        isPending = false,
        isEnabled = function()
            return JNI:context(android.app.activity.vm, function(jni)
                return jni:callBooleanMethod(
                    android.app.activity.clazz,
                    "hasOTAUpdates",
                    "()Z"
                )
            end)
        end,
        install = function()
            JNI:context(android.app.activity.vm, function(jni)
                jni:callVoidMethod(
                    android.app.activity.clazz,
                    "installApk",
                    "()V"
                )
            end)
        end,
    }

    android.orientation = {
        get = function()
            return JNI:context(android.app.activity.vm, function(jni)
                return jni:callIntMethod(
                    android.app.activity.clazz,
                    "getScreenOrientation",
                    "()I"
                )
            end)
        end,
        set = function(new_orientation)
            if new_orientation >= C.ASCREEN_ORIENTATION_UNSPECIFIED and
                new_orientation <= C.ASCREEN_ORIENTATION_FULL_SENSOR then
                JNI:context(android.app.activity.vm, function(jni)
                    jni:callVoidMethod(
                        android.app.activity.clazz,
                        "setScreenOrientation",
                        "(I)V",
                        ffi.new("int32_t", new_orientation)
                    )
                end)
            else
                android.LOGW("ignoring invalid orientation", new_orientation)
            end
        end,
    }

    android.enableFrontlightSwitch = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "enableFrontlightSwitch",
                "()Z"
            )
        end)
    end

    android.getScreenBrightness = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenBrightness",
                "()I"
            )
        end)
    end

    android.getScreenMinBrightness = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenMinBrightness",
                "()I"
            )
        end)
    end

    android.getScreenMaxBrightness = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenMaxBrightness",
                "()I"
            )
        end)
    end

    android.setScreenBrightness = function(brightness)
        android.DEBUG("set screen brightness "..brightness)
        JNI:context(android.app.activity.vm, function(jni)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "setScreenBrightness",
                "(I)V",
                ffi.new('int32_t', brightness)
                -- Note that JNI won't covert lua number to int, we need to convert
                -- it explictly.
            )
        end)
    end

    android.getScreenWarmth = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenWarmth",
                "()I"
            )
        end)
    end

    android.getScreenMinWarmth = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenMinWarmth",
                "()I"
            )
        end)
    end

    android.getScreenMaxWarmth = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getScreenMaxWarmth",
                "()I"
            )
        end)
    end

    android.setScreenWarmth = function(warmth)
        android.DEBUG("set screen warmth "..warmth)
        JNI:context(android.app.activity.vm, function(jni)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "setScreenWarmth",
                "(I)V",
                ffi.new('int32_t', warmth)
            )
        end)
    end

    android.isWarmthDevice = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "isWarmthDevice",
                "()Z"
            )
        end)
    end

    android.getBatteryLevel = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getBatteryLevel",
                "()I"
            )
        end)
    end

    android.isCharging = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "isCharging",
                "()Z"
            )
        end)
    end

    android.isTv = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "isTv",
                "()Z"
            )
        end)
    end

    android.isChromeOS = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "isChromeOS",
                "()Z"
            )
        end)
    end

    android.hasNativeRotation = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "hasNativeRotation",
                "()Z"
            )
        end)
    end

    android.getPlatformName = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local platform = jni:callObjectMethod(
                android.app.activity.clazz,
                "getPlatformName",
                "()Ljava/lang/String;"
            )
            return jni:to_string(platform)
        end)
    end

    android.isFullscreen = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "isFullscreen",
                "()Z"
            )
        end)
    end

    android.setFullscreen = function(fullscreen)
        android.DEBUG("setting fullscreen to " .. tostring(fullscreen))
        JNI:context(android.app.activity.vm, function(jni)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "setFullscreen",
                "(Z)V",
                ffi.new("bool", fullscreen)
            )
        end)
    end

    android.isEink = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local is_supported = jni:callBooleanMethod(
                android.app.activity.clazz,
                "isEink",
                "()Z"
            )

            local platform = jni:callObjectMethod(
                android.app.activity.clazz,
                "getEinkPlatform",
                "()Ljava/lang/String;"
            )
            return is_supported, jni:to_string(platform)
        end)
    end

    android.isEinkFull = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "isEinkFull",
                "()Z"
            )
        end)
    end

    android.getEinkConstants = function()
        local isEink, platform = android.isEink()
        if not isEink then return end
        local full, partial, full_ui, partial_ui, fast, delay, delay_ui, delay_fast
        if platform == "rockchip" then
            -- rockchip devices are dumb and just support updates to the entire screen.
            -- see https://github.com/koreader/android-luajit-launcher/blob/f2d946b3b49e728272df4cb56185a2fe57cdb4ff/app/src/org/koreader/launcher/Device.kt#L94-L101
            full, partial, full_ui, partial_ui, fast = 1, 2, 4, 4, 3
        else
            -- freescale/qualcomm
            local mode_full, mode_partial = 32, 0
            local wf_du, wf_gc16 = 1, 2
            delay_fast = 0
            fast, partial = wf_du + mode_partial, wf_gc16 + mode_partial
            if platform == "freescale" then
                local wf_regal = 7
                full = wf_gc16 + mode_full
                full_ui = wf_regal + mode_full
                partial_ui = wf_regal + mode_partial
                delay, delay_ui = 0, 0
            elseif platform == "qualcomm" then
                local wf_regal = 6
                local mode_wait = 64
                full = wf_gc16 + mode_full + mode_wait
                full_ui = wf_regal + mode_full
                partial_ui = wf_gc16 + mode_partial
                delay, delay_ui = 250, 100
            end
        end
        return full, partial, full_ui, partial_ui, fast, delay, delay_ui, delay_fast
    end

    android.needsWakelocks = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "needsWakelocks",
                "()Z"
            )
        end)
    end

    android.einkUpdate = function(mode)
        if not mode then return end
        JNI:context(android.app.activity.vm, function(jni)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "einkUpdate",
                "(I)V",
                ffi.new("int32_t", mode)
            )
        end)
    end

    android.einkUpdate = function(mode, delay, x, y, w, h)
        if not mode then return end
        if not (delay and x and y and w and h) then
            -- basic support, only fullscreen refreshes
            JNI:context(android.app.activity.vm, function(jni)
                jni:callVoidMethod(
                    android.app.activity.clazz,
                    "einkUpdate",
                    "(I)V",
                    ffi.new("int32_t", mode)
                )
            end)
        else
            -- full support
            JNI:context(android.app.activity.vm, function(jni)
                jni:callVoidMethod(
                    android.app.activity.clazz,
                    "einkUpdate",
                    "(IJIIII)V",
                    ffi.new("int32_t", mode),
                    ffi.new("int64_t", delay),
                    ffi.new("int32_t", x),
                    ffi.new("int32_t", y),
                    ffi.new("int32_t", w),
                    ffi.new("int32_t", h)
                )
            end)
        end
    end

    android.einkTest = function()
        JNI:context(android.app.activity.vm, function(jni)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "startEPDTestActivity",
                "()V"
            )
        end)
    end

    --- Android permission check.
    -- @treturn bool hasWriteSettingsPermission
    android.canWriteSettings = function()
        android.DEBUG("checking write settings permission")
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "hasWriteSettingsPermission",
                "()Z"
            )
        end)
    end

    android.canWriteStorage = function()
        android.DEBUG("checking write storage permission")
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "hasExternalStoragePermission",
                "()Z"
            )
        end)
    end

    android.getClipboardText = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local text = jni:callObjectMethod(
                android.app.activity.clazz,
                "getClipboardText",
                "()Ljava/lang/String;"
            )
            return jni:to_string(text)
        end)
    end

    android.hasClipboardText = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "hasClipboardText",
                "()Z"
            )
        end)
    end

    android.setClipboardText = function(text)
        android.DEBUG("setting clipboard text to: " .. text)
        JNI:context(android.app.activity.vm, function(jni)
            local clipboard_text = jni.env[0].NewStringUTF(jni.env, text)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "setClipboardText",
                "(Ljava/lang/String;)V",
                clipboard_text
            )
            jni.env[0].DeleteLocalRef(jni.env, clipboard_text)
        end)
    end

    android.getNetworkInfo = function()
        return JNI:context(android.app.activity.vm, function(jni)
            local network_info = jni:callObjectMethod(
                android.app.activity.clazz,
                "getNetworkInfo",
                "()Ljava/lang/String;"
            )
            return string.match(jni:to_string(network_info), "(.*);(.*)")
        end)
    end

    android.openWifiSettings = function()
        android.DEBUG("open android settings")
        JNI:context(android.app.activity.vm, function(jni)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "openWifiSettings",
                "()V"
            )
        end)
    end

    android.download = function(url, name)
        return JNI:context(android.app.activity.vm, function(jni)
            local uri_string = jni.env[0].NewStringUTF(jni.env, url)
            local download_name = jni.env[0].NewStringUTF(jni.env, name)
            local ret = jni:callIntMethod(
                android.app.activity.clazz,
                "download",
                "(Ljava/lang/String;Ljava/lang/String;)I",
                uri_string, download_name
            )
            jni.env[0].DeleteLocalRef(jni.env, uri_string)
            jni.env[0].DeleteLocalRef(jni.env, download_name)
            return ret
        end)
    end

    android.openLink = function(link)
        return JNI:context(android.app.activity.vm, function(jni)
            local uri_string = jni.env[0].NewStringUTF(jni.env, link)
            local result = jni:callIntMethod(
                android.app.activity.clazz,
                "openLink",
                "(Ljava/lang/String;)I",
                uri_string
            )
            jni.env[0].DeleteLocalRef(jni.env, uri_string)
            return result
        end)
    end

    android.isPackageEnabled = function(package)
        if not package then return false end
        return JNI:context(android.app.activity.vm, function(jni)
            local _package = jni.env[0].NewStringUTF(jni.env, package)
            local enabled = jni:callBooleanMethod(
                android.app.activity.clazz,
                "isPackageEnabled",
                "(Ljava/lang/String;)Z",
                _package
            )
            jni.env[0].DeleteLocalRef(jni.env, _package)
            return enabled
        end)
    end

    android.isResumed = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callBooleanMethod(
                android.app.activity.clazz,
                "isActivityResumed",
                "()Z"
            )
        end)
    end

    -- legacy call for frontend: text, package, action
    android.dictLookup = function(text, package, action)
        JNI:context(android.app.activity.vm, function(jni)
            local _text = jni.env[0].NewStringUTF(jni.env, text)
            local _action = jni.env[0].NewStringUTF(jni.env, action)
            local _package = jni.env[0].NewStringUTF(jni.env, package)
            -- updated call: text, action, package
            jni:callVoidMethod(
                android.app.activity.clazz,
                "dictLookup",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                _text, _action, _package
            )
            jni.env[0].DeleteLocalRef(jni.env, _text)
            jni.env[0].DeleteLocalRef(jni.env, _action)
            jni.env[0].DeleteLocalRef(jni.env, _package)
        end)
    end

    android.sendText = function(text)
        JNI:context(android.app.activity.vm, function(jni)
            local _text = jni.env[0].NewStringUTF(jni.env, text)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "sendText",
                "(Ljava/lang/String;)V",
                _text
            )
            jni.env[0].DeleteLocalRef(jni.env, _text)
        end)
    end

    android.notification = function(message, is_long)
        return JNI:context(android.app.activity.vm, function(jni)
            local text = jni.env[0].NewStringUTF(jni.env, message)
            jni:callVoidMethod(
                android.app.activity.clazz,
                "showToast",
                "(Ljava/lang/String;Z)V",
                text,
                ffi.new("bool", is_long and true or false)
            )
            jni.env[0].DeleteLocalRef(jni.env, text)
        end)
    end

    android.getStatusBarHeight = function()
        return JNI:context(android.app.activity.vm, function(jni)
            return jni:callIntMethod(
                android.app.activity.clazz,
                "getStatusBarHeight",
                "()I"
            )
        end)
    end

    android.untar = function(file, output)
        if not file or not output then return false end
        return JNI:context(android.app.activity.vm, function(jni)
            local i = jni.env[0].NewStringUTF(jni.env, file)
            local o = jni.env[0].NewStringUTF(jni.env, output)
            local ok = jni:callBooleanMethod(
                android.app.activity.clazz,
                "untar",
                "(Ljava/lang/String;Ljava/lang/String;)Z",
                i, o
            )
            jni.env[0].DeleteLocalRef(jni.env, i)
            jni.env[0].DeleteLocalRef(jni.env, o)
            return ok
        end)
    end

    local function subprocess(jni, argv)
        local args_array = jni.env[0].NewObjectArray(jni.env, #argv,
            jni.env[0].FindClass(jni.env, "java/lang/String"), nil)
        local args_list = {}
        for i = 1, #argv do
            local arg = jni.env[0].NewStringUTF(jni.env, argv[i])
            table.insert(args_list, arg)
            jni.env[0].SetObjectArrayElement(jni.env, args_array, i-1, arg)
        end
        local process = jni:callObjectMethod(
            jni:callStaticObjectMethod(
                "java/lang/Runtime",
                "getRuntime",
                "()Ljava/lang/Runtime;"
            ),
            "exec",
            "([Ljava/lang/String;)Ljava/lang/Process;",
            args_array
        )
        for _, arg in ipairs(args_list) do
            jni.env[0].DeleteLocalRef(jni.env, arg)
        end
        return process
    end

    android.execute = function(...)
        local argv = {...}
        return JNI:context(android.app.activity.vm, function(jni)
            local process = subprocess(jni, argv)
            local stdout = jni:callObjectMethod(
                process,
                "getInputStream",
                "()Ljava/io/InputStream;"
            )
            local stderr = jni:callObjectMethod(
                process,
                "getErrorStream",
                "()Ljava/io/InputStream;"
            )
            local out = ""
            local err = ""
            while true do
                local char = jni:callIntMethod(stdout, "read", "()I")
                if char >= 0 then
                    out = out .. string.char(char)
                else
                    break
                end
            end
            while true do
                local char = jni:callIntMethod(stderr, "read", "()I")
                if char >= 0 then
                    err = err .. string.char(char)
                else
                    break
                end
            end
            local res = jni:callIntMethod(process, "waitFor", "()I")

            if res > 0 then
                android.LOGW(string.format("command %s returned %d", table.concat(argv, " "), res))
            else
                android.LOGI(string.format("command %s returned %d", table.concat(argv, " "), res))
            end
            android.LOGV(string.format(" stdout: %s\n stderr: %s", out, err))
            jni.env[0].DeleteLocalRef(jni.env, process)
            jni.env[0].DeleteLocalRef(jni.env, stdout)
            jni.env[0].DeleteLocalRef(jni.env, stderr)
            return res
        end)
    end

    android.stdout = function(...)
        local argv = {...}
        return JNI:context(android.app.activity.vm, function(jni)
            local process = subprocess(jni, argv)
            local stdout = jni:callObjectMethod(
                process,
                "getInputStream",
                "()Ljava/io/InputStream;"
            )
            local out = ""
            while true do
                local char = jni:callIntMethod(stdout, "read", "()I")
                if char >= 0 then
                    out = out .. string.char(char)
                else
                    break
                end
            end
            jni.env[0].DeleteLocalRef(jni.env, process)
            jni.env[0].DeleteLocalRef(jni.env, stdout)
            return out
        end)
    end

    os.execute = function(command) -- luacheck: ignore 122
        if command == nil then return -1 end
        local argv = {}
        command:gsub("([^ ]+)", function(arg)
            -- strip quotes around argument, since they are not necessary here
            arg = arg:gsub('"(.*)"', "%1") -- strip double quotes
            arg = arg:gsub("'(.*)'", "%1") -- strip single quotes
            table.insert(argv, arg)
        end)
        return android.execute(unpack(argv))
    end

    -- register the "android" module (ourself)
    package.loaded.android = android

    -- set up a sensible package.path
    package.path = "?.lua;"..android.dir.."/?.lua;"
    -- set absolute cpath
    package.cpath = "?.so;"..android.dir.."/?.so;"
    -- register the asset loader
    table.insert(package.loaders, 2, android.asset_loader)

    -- load the dlopen() implementation
    android.dl = require("dl")
    android.dl.library_path = android.nativeLibraryDir..":"..
        android.dir..":"..android.dir.."/libs:"..
        "/lib:/system/lib:/lib/lib?.so:/system/lib/lib?.so"

    -- register the dependency lib loader
    table.insert(package.loaders, 3, android.deplib_loader)

    -- ffi.load wrapper
    local ffi_load = ffi.load
    ffi.load = function(library, ...) -- luacheck: ignore 212
        android.DEBUG("ffi.load "..library)
        return android.dl.dlopen(library, ffi_load)
    end

    if not android.canWriteStorage() then
        error("insufficient permissions")
    end

    local installed = android.extractAssets()
    if not installed then
        error("error extracting assets")
    end
    local launch = android.asset_loader("launcher")
    if type(launch) == "function" then
        return launch()
    else
        error("error loading launcher.lua")
    end
end

run(...)
