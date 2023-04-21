#include <jni.h>
#include <string>
#include <android/log.h>
#include <execinfo.h>
#include <cstdio>
#include <cstdlib>
#include <dlfcn.h>
#include <csignal>
#include <unwind.h>
#include <iomanip>
#include <sstream>
#include <iostream>
#include <fstream>
#include <atomic>

using namespace std;

const char *baoTag = "Bao";
const char *exception_file_path;
std::atomic<bool> caught(false);

struct BacktraceState {
    void **current;
    void **end;
};

_Unwind_Reason_Code unwindCallback(struct _Unwind_Context *context, void *arg) {
    auto *state = static_cast<BacktraceState *>(arg);
    uintptr_t pc = _Unwind_GetIP(context);
    if (pc) {
        if (state->current == state->end) {
            return _URC_END_OF_STACK;
        } else {
            *state->current++ = reinterpret_cast<void *>(pc);
        }
    }
    return _URC_NO_REASON;
}

size_t captureBacktrace(void **buffer, size_t max) {
    BacktraceState state = {buffer, buffer + max};
    _Unwind_Backtrace(unwindCallback, &state);
    return state.current - buffer;
}

void dumpBacktrace(std::ostream &os, void **buffer, size_t count) {
    for (size_t idx = 0; idx < count; ++idx) {
        const void *addr = buffer[idx];
        const char *symbol = "";
        Dl_info info;
        if (dladdr(addr, &info) && info.dli_sname) {
            symbol = info.dli_sname;
        }
        os << "#" << std::setw(2) << idx << ": " << addr << "  " << symbol << "\n";
    }
}

void backtraceToLogcat(char *info) {
    const size_t max = 30; // 调用的层数
    void *buffer[max];
    std::ostringstream oss;

    dumpBacktrace(oss, buffer, captureBacktrace(buffer, max));

    ofstream file(reinterpret_cast<const char *>(exception_file_path),
                  ios::ate); // 创建文件流
    if (file.is_open()) { // 确保文件流已打开
        string str = oss.str(); // 要写入的字符串
        file << info << endl;
        file << str; // 将字符串写入文件
        file.close(); // 关闭文件流
        __android_log_print(ANDROID_LOG_INFO, baoTag, "write done");
    } else {
        __android_log_print(ANDROID_LOG_INFO, baoTag, "写入失败 errno: %d %s %s stack: %s", errno,
                            strerror(errno), exception_file_path,
                            oss.str().c_str());
    }
}

/**
 *
 * @param sig
 * @param info
 * @param context ucontext_t
 */
static void sig_handler(int sig, struct siginfo *info, __attribute__((unused)) void *context) {
    bool expected = false;
    bool handled;
    if (caught.compare_exchange_strong(expected, true)) {
        char result[50];
        sprintf(result, "Exception captured by Bao");
        backtraceToLogcat(result);
        handled = true;
    } else {
        handled = false;
    }
    __android_log_print(ANDROID_LOG_DEBUG, baoTag, "handler sig:%d errno:%d handled:%d", sig, info->si_errno, handled);
}

extern "C" {

void registerActionHandler(__attribute__((unused)) JNIEnv *env, __attribute__((unused)) jclass thiz, jint signal) {
    struct sigaction customHandler{};
    // 自定义处理器
    customHandler.sa_sigaction = sig_handler;
    sigemptyset(&customHandler.sa_mask);
    customHandler.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_RESTART;
    // 注册信号
    int result = sigaction(signal, &customHandler, nullptr);
    __android_log_print(ANDROID_LOG_DEBUG, baoTag, "sigaction %d result %d", signal, result);
}

void transferNativeExceptionFilePath(JNIEnv *env, __attribute__((unused)) jclass thiz, jstring path) {
    exception_file_path = env->GetStringUTFChars(path, nullptr);
}

jboolean notifyExceptionCaught() {
    bool expected = false;
    return caught.compare_exchange_strong(expected, true);
}

void notifyExceptionHandled() {
    caught.store(false);
}

jboolean isCaught() {
    return caught.load();
}

}  // extern "C"

// 定义本地方法数组
static JNINativeMethod gMethods[] = {
        {"registerActionHandler",           "(I)V",                  (void *) registerActionHandler},
        {"transferNativeExceptionFilePath", "(Ljava/lang/String;)V", (void *) transferNativeExceptionFilePath},
        {"notifyExceptionCaught",           "()Z",                   (void *) notifyExceptionCaught},
        {"notifyExceptionHandled",          "()V",                   (void *) notifyExceptionHandled},
        {"isCaught",                        "()Z",                   (void *) isCaught}
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, __attribute__((unused)) void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    jclass clazz = env->FindClass("com/storyteller_f/bao_library/BaoKt");
    if (clazz == NULL) {
        return -1;
    }

    if (env->RegisterNatives(clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        return -1;
    }

    return JNI_VERSION_1_6;
}