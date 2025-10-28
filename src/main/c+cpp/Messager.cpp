#include "org_balinhui_fpa_nativeapis_Global.h"
#include <Windows.h>
#pragma comment(lib, "user32.lib")

JNIEXPORT void JNICALL Java_org_balinhui_fpa_nativeapis_Global_message
(JNIEnv *env, jclass clazz, jlong hwnd, jstring title, jstring msg) {
    const jchar* wtitle = env->GetStringChars(title, nullptr);
    const jchar* wmsg = env->GetStringChars(msg, nullptr);

    HWND window;
    if (hwnd == 0) {
        window = NULL;
    } else {
        window = (HWND) hwnd;
    }
    MessageBoxW(window,
                reinterpret_cast<LPCWSTR>(wmsg), 
                reinterpret_cast<LPCWSTR>(wtitle), 
                MB_OK);
    
    env->ReleaseStringChars(title, wtitle);
    env->ReleaseStringChars(msg, wmsg);
}
