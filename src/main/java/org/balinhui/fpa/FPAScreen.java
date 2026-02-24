package org.balinhui.fpa;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.info.SystemInfo;
import org.balinhui.fpa.nativeapis.DwmAPI;
import org.balinhui.fpa.ui.Buttons;
import org.balinhui.fpa.ui.Lyric;
import org.balinhui.fpa.ui.Windows;
import org.balinhui.fpa.util.Config;

/**
 * FPA播放器的主界面，这里编写界面的布局和各种控件，内容及窗口
 */
public class FPAScreen extends Application {
    private static final Logger logger = LogManager.getLogger(FPAScreen.class);

    /**
     * 播放器的主窗口
     */
    public static Stage mainWindow;

    /**
     * 右键菜单打开的设置窗口
     */
    public static Stage settingWindow;

    /**
     * 设置 窗口中的应用按钮
     */
    public static Button apply;

    /**
     * 设置 窗口中的取消按钮
     */
    public static Button cancel;

    /**
     * 设置 窗口中的确定按钮
     */
    public static Button ok;

    /**
     * 封面图片
     */
    public static final ImageView view = new ImageView(Resources.ImageRes.cover);

    /**
     * “选择文件” 按钮，播放歌曲时会去掉以放置歌词
     */
    public static final Button button = new Button(Resources.StringRes.button_name);

    /**
     * 歌词的面板
     */
    public static final StackPane lyricsPane = createLyricsPane();

    /**
     * 左侧的面板，其中包括封面，和一个暂停按钮
     */
    public static final VBox leftPane = new VBox();

    /**
     * 右侧的面板，其中包括选择文件按钮，歌词
     */
    public static final VBox rightPane = new VBox();

    /**
     * 根面板，是窗口最底层的面板，被分为了左右两部分
     */
    public static final AnchorPane root = new AnchorPane();

    /**
     * 进度条
     */
    public static final ProgressBar progress = new ProgressBar();

    /**
     * 右键上下文菜单
     */
    public static final ContextMenu contextMenu = createContextMenu();

    /**
     * 暂停/播放 按钮，只有在播放时才会出现
     */
    public static final Button pause = new Button();

    /**
     * 暂停按钮的图标
     */
    public static final ImageView pauseIcon = new ImageView(Resources.ImageRes.pause_black);

    /**
     * 播放按钮的图标
     */
    public static final ImageView playIcon = new ImageView(Resources.ImageRes.play_black);

    /**
     * 用来存放 暂停/播放 按钮的面板，用以将其放置在中间和后续扩展
     */
    public static final HBox control = new HBox();

    /**
     * 储存当前窗口的宽度
     */
    public static double currentWidth;

    /**
     * 储存当前窗口的高度
     */
    public static double currentHeight;

    public static final double largeSize = 450;
    public static final double mediumSize = 300;
    public static final double smallSize = 220;

    private int level = 1;//当前控件尺寸等级 共有3级

    private static int distance = 47;//进度条到底部的距离，包含窗口和控件之间的差

    private static Action action;

    @Override
    public void init() {
        logger.trace("获取Action实例，进行初始化");
        action = Action.initialize();
    }

    @Override
    public void start(Stage stage) {
        logger.trace("配置左侧面板");
        leftPane.setAlignment(Pos.CENTER);
        view.setFitWidth(smallSize);
        view.setFitHeight(smallSize);
        view.setPreserveRatio(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(view.fitWidthProperty());
        clip.heightProperty().bind(view.fitHeightProperty());
        clip.setArcHeight(10);
        clip.setArcWidth(10);

        view.setClip(clip);

        pauseIcon.setFitWidth(20);
        pauseIcon.setFitHeight(20);
        pauseIcon.setPreserveRatio(true);
        playIcon.setFitWidth(20);
        playIcon.setFitHeight(20);
        playIcon.setPreserveRatio(true);
        pause.setGraphic(pauseIcon);
        pause.setPrefWidth(100);
        Buttons.setLightOrDark(false, Buttons.Color.WHITE, pause);
        pause.setOnAction(e -> action.clickPauseButton());

        control.getChildren().add(pause);
        control.setPrefHeight(40);
        control.setAlignment(Pos.CENTER);

        leftPane.getChildren().add(view);

        logger.trace("配置右侧面板");
        rightPane.setAlignment(Pos.CENTER);
        rightPane.setPadding(new Insets(0, 10, 0, 0));
        button.setFont(Resources.FontRes.yahei_small_font);
        button.setOnAction(e -> action.clickChooseFileButton());
        Buttons.setLightOrDark(false, Buttons.Color.WHITE, button);
        rightPane.getChildren().add(button);

        progress.setPrefHeight(10);
        progress.setPrefWidth(Double.MAX_VALUE);
        progress.setStyle("-fx-accent: gray");

        logger.trace("控件添加进根面板");
        root.getChildren().addAll(leftPane, rightPane, progress);
        setAnchorPane(true);
        addFileDrop();

        double oWidth = Config.width();
        double oHeight = Config.height();
        if (oWidth == 0.0) oWidth = 600;
        if (oHeight == 0.0) oHeight = 370;
        Scene scene = new Scene(root, oWidth, oHeight);
        scene.setOnContextMenuRequested(e ->
                contextMenu.show(stage, e.getScreenX(), e.getScreenY())
        );

        stage.widthProperty().addListener((
                observableValue,
                number,
                t1) -> {
            currentWidth = t1.doubleValue();
            if (root.getChildren().contains(rightPane)) {
                double half = currentWidth / 2.0;
                AnchorPane.setRightAnchor(leftPane, half);
                AnchorPane.setLeftAnchor(rightPane, half);
            }
            changeSize();
        });
        stage.heightProperty().addListener((
                observableValue,
                number, t1) -> {
            currentHeight = t1.doubleValue();
            double height = currentHeight - distance;
            AnchorPane.setTopAnchor(progress, height);
            changeSize();
        });

        logger.trace("初始化 stage");
        if (SystemInfo.systemInfo.version >= DwmAPI.SUPPORT_API_VERSION) {
            logger.trace("将组件透明，去掉窗口装饰");
            root.setStyle("-fx-background-color: transparent");
            scene.setFill(Color.TRANSPARENT);
            stage.initStyle(StageStyle.UNIFIED);
        }
        stage.setTitle(Resources.StringRes.app_name);
        double x = Config.x();
        if (x != 0.0) stage.setX(x);
        double y = Config.y();
        if (y != 0.0) stage.setY(y);
        stage.setMinWidth(280);
        stage.setMinHeight(340);
        stage.setScene(scene);
        stage.getIcons().addAll(
                Resources.ImageRes.fpa16,
                Resources.ImageRes.fpa32,
                Resources.ImageRes.fpa64,
                Resources.ImageRes.fpa128,
                Resources.ImageRes.fpa256,
                Resources.ImageRes.fpa
        );

        stage.setOnCloseRequest(action::prepareClose);
        stage.show();
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        mainWindow = stage;
        Windows.setEffect(stage, DwmAPI.DWM_SYSTEMBACKDROP_TYPE.DWMSBT_MAINWINDOW);
        settingWindow = createSettingWindow();//等待主窗口创建完成再初始化设置窗口
    }

    @Override
    public void stop() {
        action.exit();
    }

    /**
     * 设置面板的布局
     * @param rightExist 是否存在右边的歌词面板
     */
    private void setAnchorPane(boolean rightExist) {
        double wDistance = currentWidth == 0.0 ? 300.0 : currentWidth / 2.0;
        double hDistance = currentHeight == 0.0 ? 360.0 : currentHeight - 47.0;

        AnchorPane.setLeftAnchor(leftPane, 0.0);
        AnchorPane.setLeftAnchor(progress, 0.0);

        AnchorPane.setRightAnchor(progress, 0.0);

        AnchorPane.setTopAnchor(leftPane, 0.0);
        AnchorPane.setTopAnchor(progress, hDistance);

        AnchorPane.setBottomAnchor(leftPane, 10.0);
        AnchorPane.setBottomAnchor(progress, 0.0);

        if (rightExist) {
            AnchorPane.setLeftAnchor(rightPane, wDistance);

            AnchorPane.setRightAnchor(rightPane, 0.0);
            AnchorPane.setRightAnchor(leftPane, wDistance);

            AnchorPane.setTopAnchor(rightPane, 0.0);
            AnchorPane.setBottomAnchor(rightPane, 10.0);
        } else {
            AnchorPane.setRightAnchor(leftPane, 0.0);
        }
    }

    private void addFileDrop() {
        //设置文件拖入时的效果
        root.setOnDragEntered(dragEvent -> {
            //添加模糊效果
            BoxBlur blur = new BoxBlur();
            blur.setWidth(5);
            blur.setHeight(5);
            blur.setIterations(1);

            //添加背景压暗效果
            ColorAdjust darken = new ColorAdjust();
            darken.setBrightness(-0.2);

            blur.setInput(darken);
            root.setEffect(blur);
            dragEvent.consume();
        });

        //文件离开时取消效果
        root.setOnDragExited(dragEvent -> {
            root.setEffect(null);
            dragEvent.consume();
        });

        root.setOnDragOver(action::onDragOver);

        root.setOnDragDropped(action::onDragDropped);
    }

    private static StackPane createLyricsPane() {
        //创建歌词布局
        StackPane lyricsPane = new StackPane();
        lyricsPane.setPrefWidth(smallSize);
        lyricsPane.setPrefHeight(smallSize);
        lyricsPane.setPadding(new Insets(0, 0, 10, 0));
        switch (Config.location()) {
            case "left" -> lyricsPane.setAlignment(Pos.BOTTOM_LEFT);
            case "right" -> lyricsPane.setAlignment(Pos.BOTTOM_RIGHT);
            default -> lyricsPane.setAlignment(Pos.BOTTOM_CENTER);
        }
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(lyricsPane.widthProperty());
        clip.heightProperty().bind(lyricsPane.heightProperty());
        lyricsPane.setClip(clip);
        return lyricsPane;
    }

    /**
     * 响应式布局(不是)
     * 有三个大小等级，判断通过{@code level}做了严格限制，减少重复调用，节约性能
     */
    private void changeSize() {
        if ((currentWidth > 1000 && currentHeight > 618)
                && level != 3) {
            rightPane.setPadding(new Insets(0, 30, 0, 0));
            view.setFitHeight(largeSize);
            view.setFitWidth(largeSize);
            changeLyricsPaneSize(largeSize);
            button.setFont(Resources.FontRes.yahei_large_font);
            level = 3;
        } else if (((currentWidth > 700 && currentWidth <= 1000) &&
                (currentHeight > 430 && currentHeight <= 618)) &&
                level != 2) {
            rightPane.setPadding(new Insets(0, 20, 0, 0));
            view.setFitHeight(mediumSize);
            view.setFitWidth(mediumSize);
            changeLyricsPaneSize(mediumSize);
            button.setFont(Resources.FontRes.yahei_medium_font);
            level = 2;
        } else if ((currentWidth <= 700 || currentHeight <= 430) &&
                level != 1) {
            rightPane.setPadding(new Insets(0, 10, 0, 0));
            view.setFitHeight(smallSize);
            view.setFitWidth(smallSize);
            changeLyricsPaneSize(smallSize);
            button.setFont(Resources.FontRes.yahei_small_font);
            level = 1;
        }

        if (currentWidth < 494) {
            if (root.getChildren().size() == 3) {
                root.getChildren().remove(1);
                setAnchorPane(false);
                view.setOnMouseClicked(event -> action.clickChooseFileButton());
            }
        } else {
            if (root.getChildren().size() == 2) {
                root.getChildren().add(1, rightPane);
                setAnchorPane(true);
                view.setOnMouseClicked(null);
            }
        }
    }

    /**
     * 变更歌词界面大小
     * @param size 大小
     */
    private void changeLyricsPaneSize(double size) {
        lyricsPane.setPrefWidth(size);
        lyricsPane.setPrefHeight(size);
        Lyric.setCurrentSize(size);
        for (Lyric lyric : action.getLyricList()) {
            if (size == smallSize) lyric.setSmall();
            else if (size == mediumSize) lyric.setMedium();
            else if (size == largeSize) lyric.setLarge();
            if (action.getLyricList().size() == 1) {
                if (!lyricsPane.getChildren().isEmpty() &&
                        lyric.getLabel() == lyricsPane.getChildren().getFirst())
                    lyric.mediumLocation();
            } else {
                if (!lyricsPane.getChildren().isEmpty() &&
                        lyric.getLabel() == lyricsPane.getChildren().getFirst())
                    lyric.topLocation();
                else if (lyricsPane.getChildren().size() > 1 &&
                        lyric.getLabel() == lyricsPane.getChildren().get(1))
                    lyric.mediumLocation();
            }
        }
    }

    /**
     * 右键上下文菜单
     * @return ContextMenu实例
     */
    private static ContextMenu createContextMenu() {
        String lightMenu = "-fx-background-radius: 5;" +
                "-fx-border-radius: 5;" +
                "-fx-border-color: #ccc;" +
                "-fx-border-width: 1;" +
                "-fx-background-color: white;" +
                "-fx-padding: 3;";
        String darkMenu = "-fx-background-radius: 5;" +
                "-fx-border-radius: 5;" +
                "-fx-border-color: #ccc;" +
                "-fx-border-width: 1;" +
                "-fx-background-color: black;" +
                "-fx-padding: 3;";
        String lightItem = "-fx-text-fill: black;";
        String darkItem = "-fx-text-fill: white;";
        ContextMenu menu = new ContextMenu();//总菜单
        menu.setStyle(lightMenu);

        //创建菜单项
        MenuItem openSetting = new MenuItem(Resources.StringRes.setting_item);
        CheckMenuItem fullScreen = new CheckMenuItem(Resources.StringRes.full_screen_item);
        fullScreen.setSelected(false);
        CheckMenuItem openDark = new CheckMenuItem(Resources.StringRes.open_dark_mode_item);
        openDark.setSelected(false);
        CheckMenuItem alwaysOnTop = new CheckMenuItem(Resources.StringRes.always_on_top_item);
        alwaysOnTop.setSelected(false);
        Menu moreStylesMenu = new Menu(Resources.StringRes.more_styles_item);

        //设置菜单项
        openSetting.setOnAction(actionEvent ->
            settingWindow.showAndWait());
        fullScreen.selectedProperty().addListener((
                observable,
                oldValue, newValue) -> {
            if (newValue) distance = 10;
            else distance = 47;
            mainWindow.setFullScreen(newValue);
        });
        openDark.selectedProperty().addListener((
                observableValue,
                old, open) -> {
            Buttons.setLightOrDark(open, Buttons.Color.WHITE, button, pause);//白色按钮背景变色
            Buttons.setLightOrDark(open, Buttons.Color.BLUE, apply);//蓝色按钮背景变色
            Windows.setLightOrDark(mainWindow, open);//窗口背景变色
            Lyric.setDark(open);//歌词默认颜色改变
            for (Lyric lyric : action.getLyricList()) {//当前歌词颜色改变
                int index = 1;
                if (lyricsPane.getChildren().size() == 1) index = 0;
                if (lyric.getLabel() == lyricsPane.getChildren().get(index))
                    lyric.setModeForHighLight(open);
                else lyric.setMode(open);
            }
            if (open) {//右键菜单，资源图片改变
                playIcon.setImage(Resources.ImageRes.play_white);
                pauseIcon.setImage(Resources.ImageRes.pause_white);
                menu.setStyle(darkMenu);
                for (MenuItem item : menu.getItems()) {
                    item.setStyle(darkItem);
                }
            } else {
                playIcon.setImage(Resources.ImageRes.play_black);
                pauseIcon.setImage(Resources.ImageRes.pause_black);
                menu.setStyle(lightMenu);
                for (MenuItem item : menu.getItems()) {
                    item.setStyle(lightItem);
                }
            }
        });
        alwaysOnTop.selectedProperty().addListener((
                observable,
                oldValue, newValue) ->
                mainWindow.setAlwaysOnTop(newValue)
        );

        MenuItem mainWindowStyle = new MenuItem(Resources.StringRes.mica_style);
        MenuItem transientWindowStyle = new MenuItem(Resources.StringRes.transient_style);
        MenuItem tabbedWindowStyle = new MenuItem(Resources.StringRes.tabbed_style);

        mainWindowStyle.setOnAction(actionEvent ->
                Windows.setEffect(mainWindow, DwmAPI.DWM_SYSTEMBACKDROP_TYPE.DWMSBT_MAINWINDOW)
        );
        transientWindowStyle.setOnAction(actionEvent ->
                Windows.setEffect(mainWindow, DwmAPI.DWM_SYSTEMBACKDROP_TYPE.DWMSBT_TRANSIENTWINDOW)
        );
        tabbedWindowStyle.setOnAction(actionEvent ->
                Windows.setEffect(mainWindow, DwmAPI.DWM_SYSTEMBACKDROP_TYPE.DWMSBT_TABBEDWINDOW)
        );

        moreStylesMenu.getItems().addAll(
                mainWindowStyle,
                transientWindowStyle,
                tabbedWindowStyle
        );

        menu.getItems().addAll(
                openSetting,
                new SeparatorMenuItem(),
                fullScreen,
                alwaysOnTop,
                openDark,
                moreStylesMenu
        );
        return menu;
    }

    private Stage createSettingWindow() {
        Stage instance = new Stage();
        HBox root = new HBox(10);
        Scene scene = new Scene(root);

        if (SystemInfo.systemInfo.version >= DwmAPI.SUPPORT_API_VERSION) {
            root.setStyle("-fx-background-color: transparent");
            scene.setFill(Color.TRANSPARENT);
            instance.initStyle(StageStyle.UNIFIED);
        }
        instance.setTitle(Resources.StringRes.setting_item);
        instance.initModality(Modality.APPLICATION_MODAL);//设置为应用模态
        instance.initOwner(mainWindow);

        instance.setWidth(500);
        instance.setHeight(350);
        instance.setResizable(false);
        instance.getIcons().addAll(
                Resources.ImageRes.fpa16,
                Resources.ImageRes.fpa32,
                Resources.ImageRes.fpa64,
                Resources.ImageRes.fpa128,
                Resources.ImageRes.fpa256,
                Resources.ImageRes.fpa
        );


        root.setPadding(new Insets(10));
        root.setAlignment(Pos.BOTTOM_RIGHT);

        apply = new Button(Resources.StringRes.apply_button_name);
        Buttons.setLightOrDark(false, Buttons.Color.BLUE, apply);
        apply.setPrefWidth(80);
        apply.setFont(Resources.FontRes.yahei_super_small_font);
        apply.setOnAction(actionEvent -> action.clickSettingApplyButton());

        cancel = new Button(Resources.StringRes.cancel_button_name);
        Buttons.setLightOrDark(false, Buttons.Color.WHITE, cancel);
        cancel.setPrefWidth(80);
        cancel.setFont(Resources.FontRes.yahei_super_small_font);
        cancel.setOnAction(actionEvent -> settingWindow.close());

        ok = new Button(Resources.StringRes.ok_button_name);
        Buttons.setLightOrDark(false, Buttons.Color.WHITE, ok);
        ok.setPrefWidth(80);
        ok.setFont(Resources.FontRes.yahei_super_small_font);

        root.getChildren().addAll(ok, cancel, apply);

        instance.setScene(scene);

        return instance;
    }
}
