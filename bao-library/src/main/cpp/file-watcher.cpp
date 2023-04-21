#include <jni.h>
#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <unistd.h>
#include <sys/inotify.h>
#include <android/log.h>
#include <cstring>
#include <pthread.h>
#include <map>

#define EVENT_SIZE (sizeof(struct inotify_event))
#define BUF_LEN (1024 * (EVENT_SIZE + 16))

const char *tag = "file-watcher";
std::map<int, int> watcherPair;
struct WatcherContext {
    JNIEnv *env;
    jobject thiz;
    jmethodID send;
    int fd;
};

void *my_thread_function(void *arg) {
    auto *context = (struct WatcherContext *) arg;
    char buffer[BUF_LEN];

    while (true) {
        int length = read(context->fd, buffer, BUF_LEN);
        if (length == -1) {
            __android_log_print(ANDROID_LOG_DEBUG, tag, "read buffer failed %s", strerror(errno));
            break;
        }

        // Process events
        struct inotify_event *event;
        for (char *ptr = buffer;
             ptr < buffer + length;
             ptr += sizeof(struct inotify_event) + event->len) {
            event = (struct inotify_event *) ptr;
            jlong mask = event->mask;
           context->env->CallVoidMethod(context->thiz, context->send, mask);
        }
    }
    return nullptr;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_storyteller_1f_bao_1library_FileWatcher_startNative(JNIEnv *env, jobject thiz,
                                                             jstring path) {
    jclass cl = env->GetObjectClass(thiz);
    jmethodID sendFileEvent = env->GetMethodID(cl, "onEvent", "(J)V");
    int fd = inotify_init();
    if (fd == -1) {
        __android_log_print(ANDROID_LOG_DEBUG, tag, "init failed %s", strerror(errno));
        return -1;
    }
    const char *p = env->GetStringUTFChars(path, nullptr);

    int wd = inotify_add_watch(fd, p, IN_MODIFY | IN_CREATE | IN_DELETE);
    env->ReleaseStringUTFChars(path, p);
    if (wd == -1) {
        close(fd);
        __android_log_print(ANDROID_LOG_DEBUG, tag, "add watch failed %s", strerror(errno));
        return -1;
    }

    pthread_t thread;
    struct WatcherContext context{
        env, thiz, sendFileEvent, fd
    };
    int result = pthread_create(&thread, nullptr, my_thread_function, &context);
    if (result != 0) {
        inotify_rm_watch(fd, wd);
        close(fd);
        return -1;
    }
    pthread_detach(thread);
    watcherPair[fd] = wd;
    return fd;

}
extern "C"
JNIEXPORT void JNICALL
Java_com_storyteller_1f_bao_1library_FileWatcher_stopNative(JNIEnv *env, jobject thiz, jint ptr) {
    int fd = ptr;
    int wd = watcherPair[fd];
    inotify_rm_watch(fd, wd);
    close(fd);
    watcherPair.erase(fd);
}