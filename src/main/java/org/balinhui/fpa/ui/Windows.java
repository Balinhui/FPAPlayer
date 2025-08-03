package org.balinhui.fpa.ui;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import javafx.stage.Stage;

public class Windows {
    private static final DwmAPI api = DwmAPI.INSTANCE;
    private static WinDef.HWND hWnd;
    private static Stage stageCache;

    public static void setEffect(Stage stage, DwmAPI.DWM_SYSTEMBACKDROP_TYPE type) {
        hWnd = getHWND(stage);
        extendFrameIntoClientArea(hWnd, -1, -1, -1, -1);
        int pvAttribute;
        switch (type) {
            case DWMSBT_AUTO -> pvAttribute = 0;
            case DWMSBT_NONE -> pvAttribute = 1;
            case DWMSBT_MAINWINDOW -> pvAttribute = 2;
            case DWMSBT_TRANSIENTWINDOW -> pvAttribute = 3;
            case DWMSBT_TABBEDWINDOW -> pvAttribute = 4;
            default -> throw new RuntimeException();
        }
        setWindowAttribute(hWnd, DwmAPI.DWMWA_SYSTEMBACKDROP_TYPE, pvAttribute);
    }

    public static void setLightOrDark(Stage stage, boolean pvAttribute) {
        hWnd = getHWND(stage);
        setWindowAttribute(hWnd, DwmAPI.DWMWA_USE_IMMERSIVE_DARK_MODE, pvAttribute ? 1 : 0);
    }

    public static void extendFrame(Stage stage, int left, int right, int top, int bottom) {
        hWnd = getHWND(stage);
        extendFrameIntoClientArea(hWnd, left, right, top, bottom);
    }

    private static void extendFrameIntoClientArea(WinDef.HWND hWnd, int left, int right, int top, int bottom) {
        DwmAPI.Margins margins = new DwmAPI.Margins(left, right, top, bottom);
        if (api.DwmExtendFrameIntoClientArea(hWnd, margins).intValue() != 0) {
            throw new RuntimeException("设置工作区失败");
        }
    }

    private static void setWindowAttribute(WinDef.HWND hWnd, int dwAttribute, int pvAttribute) {
        IntByReference type = new IntByReference(pvAttribute);
        if (api.DwmSetWindowAttribute(hWnd, dwAttribute, type, 4).intValue() != 0) {
            throw new RuntimeException("设置效果失败");
        }
    }

    private static WinDef.HWND getHWND(Stage stage) {
        WinDef.HWND newHWND;
        if (stage.equals(stageCache)) {
            newHWND = hWnd;
        } else {
            newHWND = User32.INSTANCE.FindWindow(null, stage.getTitle());
        }
        stageCache = stage;
        return newHWND;
    }
}
