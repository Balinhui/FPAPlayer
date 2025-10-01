package org.balinhui.fpa.nativeapis;

public class ITaskBarListAPI {

    static {
        System.loadLibrary("windows_taskbar");
    }

    // 进度状态常量
    public static final int TBPF_NOPROGRESS = 0;
    public static final int TBPF_INDETERMINATE = 1;
    public static final int TBPF_NORMAL = 2;
    public static final int TBPF_ERROR = 4;
    public static final int TBPF_PAUSED = 8;

    // 本地方法声明
    public static native boolean initialize();
    public static native void setProgressState(long hwnd, int state);
    public static native void setProgressValue(long hwnd, long completed, long total);
    public static native void release();
}
