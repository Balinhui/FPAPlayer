package org.balinhui.fpa.nativeapis;

public class Global {
    static {
        System.loadLibrary("file_chooser");
        System.loadLibrary("message");
    }

    public static native String[] chooseFiles();

    /**
     * 调用系统的消息对话框
     * @param hwnd 窗口句柄
     * @param title 对话框标题
     * @param msg 对话框内容
     * @param type 在MessageFlags中，用以指示显示的按钮，图标等
     *
     * @return 有按钮的话返回选择的结果
     */
    public static native int message(long hwnd, String title, String msg, long type);
}
