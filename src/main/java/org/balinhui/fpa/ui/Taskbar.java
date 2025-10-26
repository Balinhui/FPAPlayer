package org.balinhui.fpa.ui;

import javafx.stage.Stage;
import org.balinhui.fpa.nativeapis.ITaskBarListAPI;
import org.balinhui.fpa.util.Win32;

public class Taskbar {
    private static long hwnd;
    private static Stage stageCache;
    private static boolean initSucceed;//成功为true，失败为false

    public static boolean init() {
        initSucceed = ITaskBarListAPI.initialize();
        return initSucceed;
    }

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

    public static void release() {
        if (!initSucceed) return;
        //TODO 已知问题：播放多首歌中途退出会引发异常，暂不知原因
        ITaskBarListAPI.setProgressState(hwnd, ITaskBarListAPI.TBPF_NOPROGRESS);
        ITaskBarListAPI.release(hwnd);
    }
}
