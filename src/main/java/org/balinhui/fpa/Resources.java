package org.balinhui.fpa;

import javafx.scene.image.Image;
import javafx.scene.text.Font;

@SuppressWarnings("all")
public class Resources {
    public static class ImageRes {
        public static final Image cover = new Image(Resources.class.getResourceAsStream("/cover.png"));
    }
    public static class FontRes {
        public static final Font small_font = Font.loadFont(Resources.class.getResourceAsStream("/font/NotoSansSC_bold.ttf"), 15);
        public static final Font medium_font = Font.loadFont(Resources.class.getResourceAsStream("/font/NotoSansSC_bold.ttf"), 20);
        public static final Font large_font = Font.loadFont(Resources.class.getResourceAsStream("/font/NotoSansSC_bold.ttf"), 30);
    }
    public static class StringRes {
        public static final String app_name = "FPA Player";
        public static final String button_name = "选择文件";
        public static final String setting_item = "设置";
        public static final String open_dark_mode_item = "暗黑模式";
        public static final String more_styles_item = "背景样式";
        public static final String mica_style = "云母样式";
        public static final String transient_style = "亚克力样式";
        public static final String tabbed_style = "tabbed样式";
    }
}
