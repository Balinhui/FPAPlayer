package org.balinhui.fpa.ui;

import javafx.stage.Stage;
import org.balinhui.fpa.nativeapis.ITaskBarListAPI;
import org.balinhui.fpa.util.Win32;

/**
 * 调用Windows本机代码，控制窗口的进度条
 */
public class Taskbar {
    private static long hwnd;
    private static Stage stageCache;
    private static boolean initSucceed;//成功为true，失败为false

    /**
     * 初始化Taskbar调用。
     * @return 初始化成功为true，失败为false
     */
    public static boolean init() {
        initSucceed = ITaskBarListAPI.initialize();
        return initSucceed;
    }

    /**
     * 设置当前进度
     * @param stage 需要设置进度的窗口
     * @param progress 需要设置的进度
     */
    public static void setProgress(Stage stage, double progress) {
        if (!initSucceed) return;
        if (stageCache == null || stageCache != stage) {
            hwnd = Win32.getLongHWND(stage);
            stageCache = stage;
        }
        if (progress < 0) {
            ITaskBarListAPI.setProgressState(hwnd, ITaskBarListAPI.TBPF_INDETERMINATE);
        } else if (progress >= 1.0) {
            ITaskBarListAPI.setProgressValue(hwnd, 1, 1);
        } else {
            ITaskBarListAPI.setProgressState(hwnd, ITaskBarListAPI.TBPF_NORMAL);
            ITaskBarListAPI.setProgressValue(hwnd, (long)(progress * 1000), 1000);
        }
    }

    /**
     * 取消Taskbar进度条状态，释放内存
     */
    public static void release() {
        if (!initSucceed) return;
        ITaskBarListAPI.setProgressState(hwnd, ITaskBarListAPI.TBPF_NOPROGRESS);
        ITaskBarListAPI.release(hwnd);
    }
}
