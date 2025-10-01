#include "org_balinhui_fpa_nativeapis_ITaskBarListAPI.h"
#include <Windows.h>
#include <ShObjIdl.h>
#pragma comment(lib, "ole32.lib")
#pragma comment(lib, "user32.lib")

static ITaskbarList3 *g_pTaskbarList = NULL;

JNIEXPORT jboolean JNICALL Java_org_balinhui_fpa_nativeapis_ITaskBarListAPI_initialize(JNIEnv *env, jclass clazz) {
    //初始化 COM
    HRESULT hr = CoInitialize(NULL);
    if (FAILED(hr)) {
        return JNI_FALSE;
    }

    //创建 ITaskbarList3 实例
    hr = CoCreateInstance(
        CLSID_TaskbarList,
        NULL,
        CLSCTX_INPROC_SERVER,
        IID_ITaskbarList3,
        (void **)&g_pTaskbarList
    );

    if (SUCCEEDED(hr) && g_pTaskbarList != NULL) {
        hr = g_pTaskbarList->HrInit();
        return SUCCEEDED(hr) ? JNI_TRUE : JNI_FALSE;
    }

    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_balinhui_fpa_nativeapis_ITaskBarListAPI_setProgressState(JNIEnv *env, jclass clazz, jlong hwnd, jint state) {
    if (hwnd == 0 || !IsWindow((HWND) hwnd)) {
        return;
    }
    if (g_pTaskbarList != NULL) {
        g_pTaskbarList->SetProgressState((HWND) hwnd, (TBPFLAG) state);
    }
}

JNIEXPORT void JNICALL Java_org_balinhui_fpa_nativeapis_ITaskBarListAPI_setProgressValue(JNIEnv *env, jclass clazz, jlong hwnd, jlong completed, jlong total) {
    if (g_pTaskbarList != NULL) {
        g_pTaskbarList->SetProgressValue((HWND) hwnd, completed, total);
    }
}

JNIEXPORT void JNICALL Java_org_balinhui_fpa_nativeapis_ITaskBarListAPI_release(JNIEnv *env, jclass clazz) {
    if (g_pTaskbarList != NULL) {
        g_pTaskbarList->Release();
        g_pTaskbarList = NULL;
    }
    CoUninitialize();
}
