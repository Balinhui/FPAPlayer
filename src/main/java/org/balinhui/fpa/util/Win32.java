package org.balinhui.fpa.util;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import javafx.stage.Stage;

/**
 * 与Windows操作系统相关的一些工具
 */
public class Win32 {
    private static HWND hWnd;//窗口句柄缓存
    private static Stage stageCache;//窗口缓存

    private Win32() {}

    /**
     * 获取窗口的句柄，HWND来自jna
     * @param stage 需要获取句柄的窗口
     * @return 窗口的句柄
     */
    public static HWND getHWND(Stage stage) {
        if (!stage.equals(stageCache)) {//如果窗口已被缓存，则直接使用缓存
            hWnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
            stageCache = stage;
        }
        return hWnd;
    }

    /**
     * 获取long类型的窗口句柄（指针），用于native接口使用
     * @param stage 需要获取句柄的窗口
     * @return 窗口的句柄
     */
    public static long getLongHWND(Stage stage) {
        return Pointer.nativeValue(getHWND(stage).getPointer());
    }
}
