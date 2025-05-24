extern "C" {

#include <limits.h>
#include <stdlib.h>

#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <jni.h>

#include <archive.h>
#include <archive_entry.h>

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "unarchive", __VA_ARGS__))


static int extract_asset(struct AAssetManager *mgr, const char *name, const char *destination)
{
    struct archive *in = NULL, *out = NULL;
    int r = ARCHIVE_FAILED;
    AAsset *asset = NULL;
    char path[PATH_MAX];

    asset = AAssetManager_open(mgr, name, AASSET_MODE_UNKNOWN);
    if (!asset)
        goto end;

    in = archive_read_new();
    archive_read_support_format_all(in);
    archive_read_support_filter_all(in);

    out = archive_write_disk_new();
    archive_write_disk_set_options(out, ARCHIVE_EXTRACT_SECURE_NODOTDOT);

    if ((r = archive_read_open_memory(in, AAsset_getBuffer(asset), AAsset_getLength64(asset)))) {
        LOGE("%s", archive_error_string(in));
        goto end;
    }

    for (struct archive_entry *entry; ; ) {
        r = archive_read_next_header(in, &entry);
        if (r == ARCHIVE_EOF)
            goto end;
        if (r < ARCHIVE_OK)
            LOGE("%s", archive_error_string(in));
        if (r < ARCHIVE_WARN)
            goto end;
        if (snprintf(path, sizeof (path), "%s/%s", destination, archive_entry_pathname(entry)) >= sizeof (path)) {
            LOGE("path to long %s", archive_entry_pathname(entry));
            r = ARCHIVE_FAILED;
            goto end;
        }
        archive_entry_set_pathname(entry, path);
        r = archive_read_extract2(in, entry, out);
        if (r < ARCHIVE_OK)
            LOGE("%s", archive_error_string(out));
        if (r < ARCHIVE_WARN)
            goto end;
    }

end:
    if (in) {
        archive_read_close(in);
        archive_read_free(in);
    }

    if (out) {
        archive_write_close(out);
        archive_write_free(out);
    }

    if (asset) {
        AAsset_close(asset);
    }

    return r;
}

JNIEXPORT jint JNICALL
Java_org_koreader_launcher_Assets_extract(JNIEnv *env,
                                          __unused jobject obj,
                                          jobject assetsManager,
                                          jstring payload,
                                          jstring output)
{
    const char *name = env->GetStringUTFChars(payload, nullptr);
    const char *destination = env->GetStringUTFChars(output, nullptr);

    int r = extract_asset(AAssetManager_fromJava(env, assetsManager), name, destination);

    env->ReleaseStringUTFChars(payload, name);
    env->ReleaseStringUTFChars(output, destination);

    return r != ARCHIVE_OK && r != ARCHIVE_EOF;
}

}
