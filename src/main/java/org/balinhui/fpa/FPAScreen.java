package org.balinhui.fpa;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.ui.Buttons;
import org.balinhui.fpa.ui.DwmAPI;
import org.balinhui.fpa.ui.Lyric;
import org.balinhui.fpa.ui.Windows;

public class FPAScreen extends Application {
    private static final Logger logger = LogManager.getLogger(FPAScreen.class);
    public static Stage mainWindow;
    public static Dialog<SettingResult> settingWindow;
    public static final ImageView view = new ImageView(Resources.ImageRes.cover);
    public static final Button button = new Button(Resources.StringRes.button_name);
    public static final StackPane lyricsPane = createLyricsPane();
    public static final VBox leftPane = new VBox();
    public static final VBox rightPane = new VBox();
    public static final AnchorPane root = new AnchorPane();
    public static final ProgressBar progress = new ProgressBar();
    public static final ContextMenu contextMenu = createContextMenu();
    public static final Button pause = new Button();
    public static final ImageView pauseIcon = new ImageView(Resources.ImageRes.pause_black);
    public static final ImageView playIcon = new ImageView(Resources.ImageRes.play_black);
    public static final HBox control = new HBox();

    public static double currentWidth;
    public static double currentHeight;

    public static final double largeSize = 450;
    public static final double mediumSize = 300;
    public static final double smallSize = 220;

    private int level = 1;//当前控件尺寸等级 共有3级

    private static int distance = 47;

    private static Action action;

    @Override
    public void init() {
        logger.trace("获取Action实例");
        action = Action.getInstance();
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
        Buttons.setLightOrDark(false, pause);
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
        Buttons.setLightOrDark(false, button);
        rightPane.getChildren().add(button);

        progress.setPrefHeight(10);
        progress.setPrefWidth(Double.MAX_VALUE);
        progress.setStyle("-fx-accent: gray");

        logger.trace("设置根面板");
        root.setStyle("-fx-background-color: transparent");
        root.getChildren().addAll(leftPane, rightPane, progress);
        setAnchorPane(true);

        Scene scene = new Scene(root, 600, 370);
        scene.setFill(Color.TRANSPARENT);

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

        logger.trace("设置stage");
        stage.initStyle(StageStyle.UNIFIED);
        stage.setTitle(Resources.StringRes.app_name);
        stage.setMinWidth(280);
        stage.setMinHeight(340);
        stage.setScene(scene);
        stage.show();
        stage.setFullScreenExitHint("");
        mainWindow = stage;
        Windows.setEffect(stage, DwmAPI.DWM_SYSTEMBACKDROP_TYPE.DWMSBT_TRANSIENTWINDOW);
        settingWindow = createSettingWindow();
    }

    @Override
    public void stop() {
        action.exit();
    }

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

    private static StackPane createLyricsPane() {
        logger.trace("创建歌词布局");
        StackPane lyricsPane = new StackPane();
        lyricsPane.setPrefWidth(smallSize);
        lyricsPane.setPrefHeight(smallSize);
        lyricsPane.setPadding(new Insets(0, 0, 10, 0));
        lyricsPane.setAlignment(Pos.BOTTOM_CENTER);
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
            }
        } else {
            if (root.getChildren().size() == 2) {
                root.getChildren().add(1, rightPane);
                setAnchorPane(true);
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
                if (lyric.getLabel() == lyricsPane.getChildren().getFirst())
                    lyric.mediumLocation();
            } else {
                if (lyric.getLabel() == lyricsPane.getChildren().getFirst())
                    lyric.topLocation();
                else if (lyric.getLabel() == lyricsPane.getChildren().get(1))
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
        Menu moreStylesMenu = new Menu(Resources.StringRes.more_styles_item);

        //设置菜单项
        openSetting.setOnAction(actionEvent -> action.settingResult(settingWindow.showAndWait()));
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
            Buttons.setLightOrDark(open, button, pause);//按钮背景变色
            Windows.setLightOrDark(mainWindow, open);//窗口背景变色
            Lyric.setDark(open);//歌词默认颜色改变
            for (Lyric lyric : action.getLyricList()) {//当前歌词颜色改变
                int index = 1;
                if (lyricsPane.getChildren().size() == 1) index = 0;
                if (lyric.getLabel() == lyricsPane.getChildren().get(index))
                    lyric.setModeForHighLight(open);
                else lyric.setMode(open);
            }
            if (open) {
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
                openDark,
                moreStylesMenu
        );
        return menu;
    }

    private Dialog<SettingResult> createSettingWindow() {
        Dialog<SettingResult> dialog = new Dialog<>();
        dialog.setTitle(Resources.StringRes.setting_item);
        dialog.setWidth(400);
        dialog.setHeight(300);

        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return SettingResult.OK;
            } else if (buttonType.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return SettingResult.APPLY;
            } else if (buttonType.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return SettingResult.CANCEL;
            }
            return null;
        });

        DialogPane pane = dialog.getDialogPane();
        StackPane context = new StackPane();

        context.setPrefWidth(400);
        context.setPrefHeight(200);
        context.getChildren().add(new Text("啥也没有"));

        pane.setContent(context);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.APPLY, ButtonType.CANCEL);

        return dialog;
    }

    public enum SettingResult {
        OK, APPLY, CANCEL
    }
}
