package org.balinhui.fpa.util;

import org.balinhui.fpa.nativeapis.Global;
import org.balinhui.fpa.nativeapis.MessageFlags;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static final Properties ppt = new Properties();
    private static final String path = ".\\config\\config.properties";
    private static final FileInputStream in;

    /*
    * 属性列表:
    * app.x:double: 窗口的横坐标
    * app.y:double: 窗口的纵坐标
    * app.width:double: 窗口宽度
    * app.height:double: 窗口高度
    *
    * lyric.location:String: 歌词位置
    */
    private static final String xKey = "app.x";
    private static final String yKey = "app.y";
    private static final String widthKey = "app.width";
    private static final String heightKey = "app.height";

    private static final String locationKey = "lyric.location";

    static {
        checkDir();
        try {
            in = new FileInputStream(path);
            ppt.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Config() {}

    private static void checkDir() {
        File dir = new File(".\\config");
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                Global.message(
                        0L,
                        "文件夹出错！",
                        "根目录的config似乎是一个文件，请检查此文件。若要启动应用程序，请移动或删除此文件！",
                        MessageFlags.DisplayButtons.OK | MessageFlags.Icons.ERROR
                );
                System.exit(130);
            }
        } else {
            if (!dir.mkdir()) {
                Global.message(
                        0L,
                        "文件夹出错！",
                        "无法创建config文件夹，请尝试重启应用或者在软件根目录手动创建！",
                        MessageFlags.DisplayButtons.OK | MessageFlags.Icons.ERROR
                );
                System.exit(130);
            }
        }

        try {
            checkFile();
        } catch (IOException e) {
            Global.message(
                    0L,
                    "文件出错！",
                    "无法创建config文件，请尝试重启应用或者在" + path + "目录手动创建！信息: " + e.getMessage(),
                    MessageFlags.DisplayButtons.OK | MessageFlags.Icons.ERROR
            );
            System.exit(130);
        }
    }

    private static void checkFile() throws IOException {
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                Global.message(
                        0L,
                        "文件出错！",
                        path + "似乎是一个文件夹，请检查此文件夹。若要启动应用程序，请移动或删除此文件夹！",
                        MessageFlags.DisplayButtons.OK | MessageFlags.Icons.ERROR
                );
                System.exit(130);
            }
        } else {
            if (!file.createNewFile()) {
                Global.message(
                        0L,
                        "文件出错！",
                        "无法创建config文件，请尝试重启应用或者在" + path + "目录手动创建！",
                        MessageFlags.DisplayButtons.OK | MessageFlags.Icons.ERROR
                );
                System.exit(130);
            }
        }
    }

    public static void store() {
        try {
            ppt.store(new FileWriter(path), "Latest update at:");
        } catch (IOException e) {
            int result = Global.message(
                    0L,
                    "错误",
                    "属性保存失败:" + e.getMessage(),
                    MessageFlags.DisplayButtons.RETRY_CANCEL | MessageFlags.Icons.ERROR
            );
            if (result == MessageFlags.ReturnValue.RETRY) {
                store();
            } else {
                System.exit(130);
            }
        }
    }

    public static void x(double value) {
        ppt.setProperty(xKey, stringOf(value));
    }

    public static double x() {
        String property = ppt.getProperty(xKey, "0.0");
        return Double.parseDouble(property);
    }

    public static void y(double value) {
        ppt.setProperty(yKey, stringOf(value));
    }

    public static double y() {
        String property = ppt.getProperty(yKey, "0.0");
        return Double.parseDouble(property);
    }

    public static void width(double value) {
        ppt.setProperty(widthKey, stringOf(value));
    }

    public static double width() {
        String property = ppt.getProperty(widthKey, "0.0");
        return Double.parseDouble(property);
    }

    public static void height(double value) {
        ppt.setProperty(heightKey, stringOf(value));
    }

    public static double height() {
        String property = ppt.getProperty(heightKey, "0.0");
        return Double.parseDouble(property);
    }

    public static void location(String value) {
        if (value.equals("center")) ppt.setProperty(locationKey, value);
        if (value.equals("left")) ppt.setProperty(locationKey, value);
        if (value.equals("right")) ppt.setProperty(locationKey, value);
    }

    public static String location() {
        return ppt.getProperty(locationKey, "center");
    }


    private static String stringOf(double d) {
        return d + "";
    }

}
