package org.balinhui.fpa;

import javafx.application.Application;
import org.balinhui.fpa.nativeapis.Global;

import java.io.File;

public class Launcher {

    public static void main(String[] args) {
        File log = new File("logs\\app.log");
        if (log.exists() && log.length() > 600000) {
            if (!log.delete()) {
                Global.message(0L, "错误", "logs文件夹下的app.log文件无法删除，尝试重启应用或手动删除文件！");
                System.exit(0);
            }
        }
        Application.launch(FPAScreen.class, args);
    }
}
