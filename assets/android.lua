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

local JNI = {}

function JNI:context(jvm, runnable)
    self.jvm = jvm

    local env = ffi.new("JNIEnv*[1]")
    self.jvm[0].GetEnv(self.jvm, ffi.cast("void**", env), ffi.C.JNI_VERSION_1_6)

    assert(self.jvm[0].AttachCurrentThread(self.jvm, env, nil) ~= ffi.C.JNI_ERR,
        "cannot attach JVM to current thread")

    self.env = env[0]
    local result = { runnable(self) }

    self.jvm[0].DetachCurrentThread(self.jvm)
    self.env = nil

    return unpack(result)
end

function JNI:callObjectMethod(object, method, signature, ...)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local methodID = self.env[0].GetMethodID(self.env, clazz, method, signature)
    return self.env[0].CallObjectMethod(self.env, object, methodID, ...)
end

function JNI:callIntMethod(object, method, signature, ...)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local methodID = self.env[0].GetMethodID(self.env, clazz, method, signature)
    return self.env[0].CallIntMethod(self.env, object, methodID, ...)
end

function JNI:getObjectField(object, field, signature)
    local clazz = self.env[0].GetObjectClass(self.env, object)
    local fieldID = self.env[0].GetFieldID(self.env, clazz, field, signature)
    return self.env[0].GetObjectField(self.env, object, fieldID)
end

function JNI:to_string(javastring)
    local utf = self.env[0].GetStringUTFChars(self.env, javastring, nil)
    local luastr = ffi.string(utf, self.env[0].GetStringUTFLength(self.env, javastring))
    self.env[0].ReleaseStringUTFChars(self.env, javastring, utf)
    return luastr
end

-- Android specific

local android = {
    app = nil,
    jni = JNI,
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
    local filename = string.gsub("?.lua", "%?", modulepath)
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
    return errmsg
end

--[[
this loader function just loads dependency libraries for C module
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
            android.LOGI("try to load module "..module)
            local ok, err = pcall(android.dl.dlopen, module)
            if ok then return end
            if err then
                android.LOGI("error: " .. err)
            end
        end
    end
end

function android.get_application_directory()
end

--[[
the C code will call this function:
--]]
local function run(android_app_state)
    android.app = ffi.cast("struct android_app*", android_app_state)

    android.dir, android.nativeLibraryDir =
        JNI:context(android.app.activity.vm, function(JNI)
            local files_dir = JNI:callObjectMethod(
                JNI:callObjectMethod(
                    android.app.activity.clazz,
                    "getFilesDir",
                    "()Ljava/io/File;"
                ),
                "getAbsolutePath",
                "()Ljava/lang/String;"
            )
            local app_info = JNI:getObjectField(
                JNI:callObjectMethod(
                    JNI:callObjectMethod(
                        android.app.activity.clazz,
                        "getPackageManager",
                        "()Landroid/content/pm/PackageManager;"
                    ),
                    "getPackageInfo",
                    "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;",
                    JNI:callObjectMethod(
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
                JNI:to_string(files_dir),
                JNI:to_string(JNI:getObjectField(app_info, "nativeLibraryDir", "Ljava/lang/String;"))
        end)
    android.screen = {}
    android.screen.width, android.screen.height =
        JNI:context(android.app.activity.vm, function(JNI)
            local display = JNI:callObjectMethod(
                JNI:callObjectMethod(
                    android.app.activity.clazz,
                    "getWindowManager",
                    "()Landroid/view/WindowManager;"
                ),
                "getDefaultDisplay",
                "()Landroid/view/Display;"
            )
            return
                JNI:callIntMethod(display, "getWidth", "()I"),
                JNI:callIntMethod(display, "getHeight", "()I")
        end)
    android.LOGI("Application data directory "..android.dir)
    android.LOGI("Application library directory "..android.nativeLibraryDir)
    android.LOGI("Screen size "..android.screen.width.."x"..android.screen.height)

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
    ffi.load = function(library, ...)
        android.LOGI("ffi.load "..library)
        return android.dl.dlopen(library, ffi_load)
    end

    -- install native libraries into libs
    local install = android.asset_loader("install")
    if type(install) == "function" then
        install()
    else
        error("error loading install.lua")
    end
    local launch = android.asset_loader("launcher")
    if type(launch) == "function" then
        return launch()
    else
        error("error loading launcher.lua")
    end
end

run(...)
