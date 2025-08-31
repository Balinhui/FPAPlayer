package org.balinhui.fpa.util;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import javafx.stage.Stage;

public class Win32 {
    private static HWND hWnd;
    private static Stage stageCache;

    private Win32() {}

    public static HWND getHWND(Stage stage) {
        if (!stage.equals(stageCache)) {
            hWnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
            stageCache = stage;
        }
        return hWnd;
    }

    public static long getLongHWND(Stage stage) {
        return Pointer.nativeValue(getHWND(stage).getPointer());
    }
}
