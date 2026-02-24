package org.balinhui.fpa.nativeapis;

import java.util.List;

public class Global {
    static {
        System.loadLibrary("file_chooser");
        System.loadLibrary("message");
    }

    /**
     * 打开一个文件选择器
     * @return 选择的文件
     */
    public static native String[] chooseFiles(String windowName, List<String> suffixNames);

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
