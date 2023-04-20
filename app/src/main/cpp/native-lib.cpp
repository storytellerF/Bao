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

extern "C"
JNIEXPORT void JNICALL
Java_com_storyteller_1f_bao_MainActivity_00024Companion_sigEnv(JNIEnv *env, jobject thiz) {
    raise(SIGSEGV);
}