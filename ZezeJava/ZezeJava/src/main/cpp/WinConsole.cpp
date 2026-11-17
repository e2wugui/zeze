#ifdef _WIN32 // only for windows

#include <windows.h>
#include <stdio.h>
#include <mutex>
#include "jni.h"

#define DEF_JAVA(F) Java_Zeze_Util_WinConsole_ ## F

static std::mutex g_lock;
static JavaVM* g_jvm = 0;
static jobject g_handler = 0;
static jmethodID g_handlerMethdId = 0;

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved)
{
    g_jvm = jvm;
    return JNI_VERSION_1_1;
}

static BOOL isServiceProcess()
{
    USEROBJECTFLAGS flags;
    HANDLE handle = GetProcessWindowStation();
    return handle && GetUserObjectInformation(handle, UOI_FLAGS, &flags, sizeof(USEROBJECTFLAGS), 0)
        && (flags.dwFlags & WSF_VISIBLE) == 0; // non-interactive session
}

static BOOL WINAPI consoleHandler(DWORD event)
{
    // fprintf(stderr, "consoleHandler: %d\n", (int)event);
    switch (event) {
    case CTRL_LOGOFF_EVENT:
        // Don't terminate JVM if it is running in a non-interactive session, such as a service process.
        if (isServiceProcess())
            break;
    case CTRL_C_EVENT:
    case CTRL_BREAK_EVENT:
    case CTRL_CLOSE_EVENT:
    case CTRL_SHUTDOWN_EVENT:
        JavaVM* jvm = g_jvm;
        if (jvm)
        {
            JNIEnv* jenv = 0;
            jint r = jvm->GetEnv((void**)&jenv, JNI_VERSION_1_1);
            bool attached = false;
            if (r == JNI_EDETACHED)
            {
                r = jvm->AttachCurrentThread((void**)&jenv, 0);
                attached = r == JNI_OK;
                if (r != JNI_OK || !jenv)
                    fprintf(stderr, "AttachCurrentThread failed: %d\n", (int)r);
            }
            if (jenv)
            {
                jobject handler;
                jmethodID handlerMethdId;
                {
                    // 与 hookCloseConsole 的全局引用替换同步，局部引用使锁外回调期间对象存活。
                    std::lock_guard<std::mutex> guard(g_lock);
                    handlerMethdId = g_handlerMethdId;
                    handler = g_handler && handlerMethdId ? jenv->NewLocalRef(g_handler) : 0;
                }
                BOOL b = handler ? (BOOL)jenv->CallBooleanMethod(handler, handlerMethdId, (jint)event) : FALSE;
                if (jenv->ExceptionCheck())
                {
                    jenv->ExceptionDescribe();
                    jenv->ExceptionClear();
                    b = FALSE;
                }
                if (handler)
                    jenv->DeleteLocalRef(handler);
                // Windows 创建的控制台线程在回调返回后退出，必须撤销本次 attach。
                // 已由 JVM 登记的线程归原调用方所有，不能 detach。
                if (attached)
                    jvm->DetachCurrentThread();
                return b;
            }
            fprintf(stderr, "GetEnv failed: %d\n", (int)r);
        }
    }
    return FALSE;
}

// public static native boolean hookCloseConsole(IntPredicate handler);
extern "C" JNIEXPORT jboolean JNICALL DEF_JAVA(hookCloseConsole)(JNIEnv* jenv, jclass jcls, jobject handler)
{
    jobject t_handler = 0;
    jmethodID t_handlerMethdId = 0;
    if (handler)
    {
        jclass c = jenv->GetObjectClass(handler);
        if (!c)
        {
            fprintf(stderr, "GetObjectClass failed: %p\n", handler);
            return JNI_FALSE;
        }
        t_handlerMethdId = jenv->GetMethodID(c, "test", "(I)Z");
        if (!t_handlerMethdId)
        {
            fprintf(stderr, "GetMethodID failed: %p\n", handler);
            return JNI_FALSE;
        }
        t_handler = jenv->NewGlobalRef(handler);
        if (!t_handler)
        {
            fprintf(stderr, "NewGlobalRef failed: %p\n", handler);
            return JNI_FALSE;
        }
    }
    g_lock.lock();
    if (g_handler)
        jenv->DeleteGlobalRef(g_handler);
    g_handler = t_handler;
    g_handlerMethdId = t_handlerMethdId;
    BOOL r = SetConsoleCtrlHandler(consoleHandler, FALSE); // ensure last handler removed
    if (handler)
        r = SetConsoleCtrlHandler(consoleHandler, TRUE);
    g_lock.unlock();
    return r ? JNI_TRUE : JNI_FALSE;
}

// public static native int getCloseConsoleTimeout(int event);
extern "C" JNIEXPORT jint JNICALL DEF_JAVA(getCloseConsoleTimeout)(JNIEnv* jenv, jclass jcls, jint event)
{
    UINT action;
    switch (event)
    {
    case CTRL_LOGOFF_EVENT:
        action = SPI_GETWAITTOKILLTIMEOUT;
        break;
    case CTRL_CLOSE_EVENT:
        action = SPI_GETHUNGAPPTIMEOUT;
        break;
    case CTRL_SHUTDOWN_EVENT:
        action = isServiceProcess() ? SPI_GETWAITTOKILLSERVICETIMEOUT : SPI_GETWAITTOKILLTIMEOUT;
        break;
    default:
        return -1;
    }
    int r;
    return SystemParametersInfo(action, 0, &r, 0) ? r : -2;
}

#endif
