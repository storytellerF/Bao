#include <jni.h>
#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <unistd.h>
#include <sys/inotify.h>
#include <android/log.h>
#include <cstring>

#define EVENT_SIZE (sizeof(struct inotify_event))
#define BUF_LEN (1024 * (EVENT_SIZE + 16))
//
// Created by fuzhengyin on 2023/4/21.
//
const char *tag = "file-watcher";
int fd, wd;

extern "C"
JNIEXPORT void JNICALL
Java_com_storyteller_1f_bao_1library_FileWatcher_startNative(JNIEnv *env, jobject thiz,
                                                             jstring path) {
    jclass cl = env->GetObjectClass(thiz);
    jmethodID m = env->GetMethodID(cl, "onEvent", "(I)V");
    fd = inotify_init();
    if (fd == -1) {
        __android_log_print(ANDROID_LOG_DEBUG, tag, "init failed %s", strerror(errno));
        return;
    }
    const char *p = env->GetStringUTFChars(path, nullptr);

    wd = inotify_add_watch(fd, p, IN_MODIFY | IN_CREATE | IN_DELETE);
    env->ReleaseStringUTFChars(path, p);
    if (wd == -1) {
        __android_log_print(ANDROID_LOG_DEBUG, tag, "add watch failed %s", strerror(errno));
        return;
    }
    char buffer[BUF_LEN];
    // Wait for events
    while (true) {
        int length = read(fd, buffer, BUF_LEN);
        if (length == -1) {
            __android_log_print(ANDROID_LOG_DEBUG, tag, "read buffer failed %s", strerror(errno));
            return;
        }

        // Process events
        struct inotify_event *event;
        for (char *ptr = buffer;
             ptr < buffer + length;
             ptr += sizeof(struct inotify_event) + event->len) {
            event = (struct inotify_event *) ptr;
            jint mask = event->mask;
            env->CallVoidMethod(thiz, m, mask);
        }
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_storyteller_1f_bao_1library_FileWatcher_stopNative(JNIEnv *env, jobject thiz) {
    inotify_rm_watch(fd, wd);
    close(fd);
}