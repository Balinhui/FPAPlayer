#include "org_balinhui_fpa_nativeapis_Global.h"
#include <Windows.h>
#include <ShObjIdl.h>
#pragma comment(lib, "user32.lib")
#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "ole32.lib")

JNIEXPORT jobjectArray JNICALL Java_org_balinhui_fpa_nativeapis_Global_chooseFiles(JNIEnv *env, jclass clazz) {
    HRESULT hr;
    IFileOpenDialog *pFileOpen;
    
    hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
    if (FAILED(hr)) return NULL;

    hr = CoCreateInstance(&CLSID_FileOpenDialog, NULL, CLSCTX_ALL, &IID_IFileOpenDialog, (void**)&pFileOpen);
    if (FAILED(hr)) {
        CoUninitialize();
        return NULL;
    }

    DWORD dwFlags;
    pFileOpen->lpVtbl->GetOptions(pFileOpen, &dwFlags);
    dwFlags |= FOS_FORCEFILESYSTEM | FOS_FILEMUSTEXIST | FOS_ALLOWMULTISELECT;
    pFileOpen->lpVtbl->SetOptions(pFileOpen, dwFlags);

    HWND hWnd = FindWindowW(NULL, L"FPA Player");
    if (hWnd == NULL) {
        CoUninitialize();
        return NULL;
    }
    hr = pFileOpen->lpVtbl->Show(pFileOpen, hWnd);
    if (FAILED(hr)) {
        pFileOpen->lpVtbl->Release(pFileOpen);
        CoUninitialize();
        return NULL;
    }

    IShellItemArray *pItems;
    hr = pFileOpen->lpVtbl->GetResults(pFileOpen, &pItems);
    if (FAILED(hr)) {
        pFileOpen->lpVtbl->Release(pFileOpen);
        CoUninitialize();
        return NULL;
    }

    DWORD count = 0;
    pItems->lpVtbl->GetCount(pItems, &count);
    jobjectArray result = (*env)->NewObjectArray(env, count, (*env)->FindClass(env, "java/lang/String"), NULL);

    for (DWORD i = 0; i < count; i++)
    {
        IShellItem *pItem;
        hr = pItems->lpVtbl->GetItemAt(pItems, i, &pItem);
        if (SUCCEEDED(hr)) {
            LPWSTR pszFilePath = NULL;
            hr = pItem->lpVtbl->GetDisplayName(pItem, SIGDN_FILESYSPATH, &pszFilePath);
            if (SUCCEEDED(hr)) {
                jstring jpath = (*env)->NewString(env, pszFilePath, wcslen(pszFilePath));
                (*env)->SetObjectArrayElement(env, result, i, jpath);
                CoTaskMemFree(pszFilePath);
            }
            pItem->lpVtbl->Release(pItem);
        }
    }
    
    pItems->lpVtbl->Release(pItems);
    pFileOpen->lpVtbl->Release(pFileOpen);
    CoUninitialize();

    return result;
}
