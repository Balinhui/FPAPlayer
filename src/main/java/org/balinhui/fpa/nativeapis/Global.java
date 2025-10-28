package org.balinhui.fpa.nativeapis;

public class Global {
    static {
        System.loadLibrary("file_chooser");
        System.loadLibrary("message");
    }

    public static native String[] chooseFiles();

    public static native void message(long hwnd, String title, String msg);
}
