package org.balinhui.fpa.ui;

import javafx.stage.Stage;
import org.balinhui.fpa.apis.ITaskBarListApi;
import org.balinhui.fpa.util.Win32;

public class Taskbar {
    private static long hwnd;
    private static Stage stageCache;
    private static boolean initSucceed;//成功为true，失败为false

    public static boolean init() {
        initSucceed = ITaskBarListApi.initialize();
        return initSucceed;
    }

    public static void setProgress(Stage stage, double progress) {
        if (!initSucceed) return;
        if (stageCache == null || stageCache != stage) {
            hwnd = Win32.getLongHWND(stage);
            stageCache = stage;
        }
        if (progress < 0) {
            ITaskBarListApi.setProgressState(hwnd, ITaskBarListApi.TBPF_INDETERMINATE);
        } else if (progress >= 1.0) {
            ITaskBarListApi.setProgressValue(hwnd, 1, 1);
        } else {
            ITaskBarListApi.setProgressState(hwnd, ITaskBarListApi.TBPF_NORMAL);
            ITaskBarListApi.setProgressValue(hwnd, (long)(progress * 1000), 1000);
        }
    }

    public static void release() {
        if (!initSucceed) return;
        ITaskBarListApi.setProgressState(hwnd, ITaskBarListApi.TBPF_NOPROGRESS);
        ITaskBarListApi.release();
    }
}
