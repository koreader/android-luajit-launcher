#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>

/*     The ioctl() system call manipulates the underlying device
       parameters of special files. */

int _ioctl(const char* device, int command, int args)
{
    int fd = open(device, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    if (fd == -1)
        return -errno;

    int code = ioctl(fd, command, args);
    int r;
    if (code == -1)
        r = -errno;
    else
        r = code;

    close(fd);
    return r;
}

JNIEXPORT jint JNICALL
Java_org_koreader_launcher_device_Ioctl_ioctl(JNIEnv *env, __unused jobject,
        jstring device, jint command, jint args)
{
    const char *dev = env->GetStringUTFChars(device, nullptr);
    jint res = _ioctl(dev, command, args);
    env->ReleaseStringUTFChars(device, dev);
    return res;
}

#ifdef __cplusplus
}
#endif
