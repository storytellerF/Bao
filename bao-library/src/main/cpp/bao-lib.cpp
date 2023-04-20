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

using namespace std;

const char *tag = "Bao";
const char* exception_file_path;

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

void backtraceToLogcat() {
    const size_t max = 30; // 调用的层数
    void *buffer[max];
    std::ostringstream oss;

    dumpBacktrace(oss, buffer, captureBacktrace(buffer, max));

    ofstream file(reinterpret_cast<const char *>(exception_file_path),
                  ios::ate); // 创建文件流
    if (file.is_open()) { // 确保文件流已打开
        string str = oss.str(); // 要写入的字符串
        file << str; // 将字符串写入文件
        file.close(); // 关闭文件流
    } else {
        __android_log_print(ANDROID_LOG_INFO, tag, "写入失败 errno: %d %s %s stack: %s", errno, strerror(errno), exception_file_path,
                            oss.str().c_str());
    }
}

static void sig_handler(int sig, struct siginfo *info, void *context) {
    char result[50];
    auto *uContext = static_cast<ucontext_t *>(context);
    sprintf(result, "handler sig:%d errno:%d flags:%lu", sig, info->si_errno, uContext->uc_flags);
    __android_log_write(ANDROID_LOG_DEBUG, tag, result);
    backtraceToLogcat();
}

static struct sigaction old;

extern "C"
JNIEXPORT void JNICALL
Java_com_storyteller_1f_bao_1library_Bao_00024Companion_registerActionHandler(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jstring exceptionFilePath) {
    struct sigaction customHandler{};
    // 自定义处理器
    customHandler.sa_sigaction = sig_handler;
    sigemptyset(&customHandler.sa_mask);
    customHandler.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_RESTART;
    // 注册信号
    int flag = sigaction(SIGSEGV, &customHandler, &old);
    printf("flag %d", flag);
    char result[30];
    sprintf(result, "flag %d", flag);
    __android_log_write(ANDROID_LOG_DEBUG, tag, result);
    exception_file_path = env->GetStringUTFChars(exceptionFilePath, nullptr);;
}