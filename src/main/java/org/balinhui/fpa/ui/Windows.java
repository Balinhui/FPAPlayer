package org.balinhui.fpa.ui;

import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import javafx.stage.Stage;
import org.balinhui.fpa.FPAScreen;
import org.balinhui.fpa.info.SystemInfo;
import org.balinhui.fpa.nativeapis.DwmAPI;
import org.balinhui.fpa.nativeapis.Global;
import org.balinhui.fpa.nativeapis.MessageFlags;
import org.balinhui.fpa.nativeapis.Share;
import org.balinhui.fpa.util.Win32;

/**
 * 设置窗口的效果的工具类，通过调用dwmapi.dll实现
 */
public class Windows {
    private static final DwmAPI api = DwmAPI.INSTANCE;


    public static void setEffect(Stage stage, DwmAPI.DWM_SYSTEMBACKDROP_TYPE type) {
        HWND hWnd = Win32.getHWND(stage);
        extendFrameIntoClientArea(hWnd, -1, -1, -1, -1);
        int pvAttribute = switch (type) {
            case DWMSBT_AUTO -> 0;
            case DWMSBT_NONE -> 1;
            case DWMSBT_MAINWINDOW -> 2;
            case DWMSBT_TRANSIENTWINDOW -> 3;
            case DWMSBT_TABBEDWINDOW -> 4;
        };
        setWindowAttribute(hWnd, DwmAPI.DWMWA_SYSTEMBACKDROP_TYPE, pvAttribute);
    }

    public static void setLightOrDark(Stage stage, boolean pvAttribute) {
        setWindowAttribute(Win32.getHWND(stage), DwmAPI.DWMWA_USE_IMMERSIVE_DARK_MODE, pvAttribute ? 1 : 0);
    }

    public static void extendFrame(Stage stage, int left, int right, int top, int bottom) {
        extendFrameIntoClientArea(Win32.getHWND(stage), left, right, top, bottom);
    }

    private static void extendFrameIntoClientArea(HWND hWnd, int left, int right, int top, int bottom) {
        DwmAPI.Margins margins = new DwmAPI.Margins(left, right, top, bottom);
        if (api.DwmExtendFrameIntoClientArea(hWnd, margins).intValue() != Share.S_OK) {
            throw new RuntimeException("设置工作区失败");
        }
    }

    private static void setWindowAttribute(HWND hWnd, int dwAttribute, int pvAttribute) {
        if (SystemInfo.systemInfo.version < DwmAPI.SUPPORT_API_VERSION) {
            Global.message(Win32.getLongHWND(FPAScreen.mainWindow), "设置失败", "当前的系统版本不支持这些效果~",
                    MessageFlags.DisplayButtons.OK | MessageFlags.Icons.WARNING);
            return;
        }
        IntByReference type = new IntByReference(pvAttribute);
        if (api.DwmSetWindowAttribute(hWnd, dwAttribute, type, 4).intValue() != Share.S_OK) {
            throw new RuntimeException("设置效果失败");
        }
    }
}
