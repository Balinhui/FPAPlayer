package org.balinhui.fpa.nativeapis;

public class ITaskBarListAPI {

    static {
        System.loadLibrary("windows_taskbar");
    }

    // 进度状态常量
    public static final int NO_PROGRESS = 0;
    public static final int INDE_TERMINATE = 1;
    public static final int NORMAL = 2;
    public static final int ERROR = 4;
    public static final int PAUSED = 8;

    // 本地方法声明
    public static native boolean initialize();
    public static native void setProgressState(long hwnd, int state);
    public static native void setProgressValue(long hwnd, long completed, long total);

    /**
     * 释放资源
     * @param hwnd 作用的窗口
     * @return 释放成功为{@code true} 否则为 {@code false}
     */
    public static native boolean release(long hwnd);
}
