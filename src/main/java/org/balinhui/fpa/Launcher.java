package org.balinhui.fpa;

import javafx.application.Application;
import org.balinhui.fpa.nativeapis.Global;

import java.io.File;

public class Launcher {

    public static void main(String[] args) {
        //检测日志文件
        File log = new File("logs\\app.log");
        //如果日志文件的大小过大，则会清除
        if (log.exists() && log.length() > 100000) {
            if (!log.delete()) {
                Global.message(0L, "错误", "logs文件夹下的app.log文件无法删除，尝试重启应用或手动删除文件！");
                System.exit(0);
            }
        }
        Application.launch(FPAScreen.class, args);
    }
}
