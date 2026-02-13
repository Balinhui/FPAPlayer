package org.balinhui.fpa;

import javafx.scene.image.Image;
import javafx.scene.text.Font;

@SuppressWarnings("all")
public class Resources {
    public static class ImageRes {
        public static final Image cover = new Image(Resources.class.getResourceAsStream("/images/cover.png"));
        public static final Image play_black = new Image(Resources.class.getResourceAsStream("/images/play_black.png"));
        public static final Image pause_black = new Image(Resources.class.getResourceAsStream("/images/pause_black.png"));
        public static final Image play_white = new Image(Resources.class.getResourceAsStream("/images/play_white.png"));
        public static final Image pause_white = new Image(Resources.class.getResourceAsStream("/images/pause_white.png"));
        public static final Image fpa = new Image(Resources.class.getResourceAsStream("/images/FPA.png"));
        public static final Image fpa16 = new Image(Resources.class.getResourceAsStream("/images/FPA16.png"));
        public static final Image fpa32 = new Image(Resources.class.getResourceAsStream("/images/FPA32.png"));
        public static final Image fpa64 = new Image(Resources.class.getResourceAsStream("/images/FPA64.png"));
        public static final Image fpa128 = new Image(Resources.class.getResourceAsStream("/images/FPA128.png"));
        public static final Image fpa256 = new Image(Resources.class.getResourceAsStream("/images/FPA256.png"));
    }
    public static class FontRes {
        public static final Font yahei_super_small_font = new Font("Microsoft YaHei", 12);
        public static final Font yahei_small_font = new Font("Microsoft YaHei", 15);
        public static final Font yahei_medium_font = new Font("Microsoft YaHei", 20);
        public static final Font yahei_large_font = new Font("Microsoft YaHei", 30);
        public static final Font noto_small_font = Font.loadFont(Resources.class.getResourceAsStream("/fonts/NotoSansSC_bold.ttf"), 15);
        public static final Font noto_medium_font = Font.loadFont(Resources.class.getResourceAsStream("/fonts/NotoSansSC_bold.ttf"), 20);
        public static final Font noto_large_font = Font.loadFont(Resources.class.getResourceAsStream("/fonts/NotoSansSC_bold.ttf"), 30);
    }
    public static class StringRes {
        public static final String app_name = "FPA Player";
        public static final String button_name = "选择文件";
        public static final String apply_button_name = "应用";
        public static final String cancel_button_name = "取消";
        public static final String ok_button_name = "确定";
        public static final String setting_item = "设置";
        public static final String full_screen_item = "全屏";
        public static final String always_on_top_item = "窗口置顶";
        public static final String open_dark_mode_item = "暗黑模式";
        public static final String more_styles_item = "背景样式";
        public static final String mica_style = "云母样式";
        public static final String transient_style = "亚克力样式";
        public static final String tabbed_style = "tabbed样式";
    }
}
