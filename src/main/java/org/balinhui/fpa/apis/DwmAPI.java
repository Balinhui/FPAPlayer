package org.balinhui.fpa.apis;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface DwmAPI extends StdCallLibrary {
    DwmAPI INSTANCE = Native.load("dwmapi", DwmAPI.class, W32APIOptions.DEFAULT_OPTIONS);

    int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    int DWMWA_SYSTEMBACKDROP_TYPE = 38;

    HRESULT DwmSetWindowAttribute(HWND hwnd, int dwAttribute, IntByReference pvAttribute, int cbAttribute);

    HRESULT DwmExtendFrameIntoClientArea(HWND hwnd, Margins margins);

    @Structure.FieldOrder({"cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight"})
    class Margins extends Structure {
        public int cxLeftWidth;
        public int cxRightWidth;
        public int cyTopHeight;
        public int cyBottomHeight;

        public Margins(int left, int right, int top, int bottom) {
            cxLeftWidth = left;
            cxRightWidth = right;
            cyTopHeight = top;
            cyBottomHeight = bottom;
        }
    }
    enum DWM_SYSTEMBACKDROP_TYPE {
        DWMSBT_AUTO,// =0
        DWMSBT_NONE,// =1
        DWMSBT_MAINWINDOW,// =2
        DWMSBT_TRANSIENTWINDOW,// =3
        DWMSBT_TABBEDWINDOW// =4
    }
}
