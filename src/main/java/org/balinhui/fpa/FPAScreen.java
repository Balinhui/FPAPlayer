package org.balinhui.fpa;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
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
    public static Dialog<String> settingWindow;
    public static final ImageView view = new ImageView(Resources.ImageRes.cover);
    public static final Button button = new Button(Resources.StringRes.button_name);
    public static final StackPane lyricsPane = createLyricsPane();
    public static final VBox leftPane = new VBox();
    public static final VBox rightPane = new VBox();
    public static final ProgressBar progress = new ProgressBar();
    public static final ContextMenu contextMenu = createContextMenu();

    public static double currentWidth;
    public static double currentHeight;

    public static final double largeSize = 450;
    public static final double mediumSize = 300;
    public static final double smallSize = 220;

    private int level = 1;//当前控件尺寸等级 共有3级

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
        leftPane.getChildren().add(view);

        logger.trace("配置右侧面板");
        rightPane.setAlignment(Pos.CENTER);
        rightPane.setPadding(new Insets(0, 10, 0, 0));
        button.setFont(Resources.FontRes.yahei_small_font);
        button.setOnAction(event -> action.ClickButton());
        Buttons.setLightOrDark(false, button);
        rightPane.getChildren().add(button);

        progress.setPrefHeight(10);
        progress.setPrefWidth(Double.MAX_VALUE);

        logger.trace("设置根面板");
        AnchorPane root = new AnchorPane();
        root.setStyle("-fx-background-color: transparent");
        root.getChildren().addAll(leftPane, rightPane, progress);
        setAnchorPane();

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
            double half = currentWidth / 2.0;
            AnchorPane.setRightAnchor(leftPane, half);
            AnchorPane.setLeftAnchor(rightPane, half);
            changeSize();
        });
        stage.heightProperty().addListener((
                observableValue,
                number, t1) -> {
            currentHeight = t1.doubleValue();
            double height = currentHeight - 47;
            AnchorPane.setTopAnchor(progress, height);
            changeSize();
        });

        logger.trace("设置stage");
        stage.initStyle(StageStyle.UNIFIED);
        stage.setTitle(Resources.StringRes.app_name);
        stage.setMinWidth(600);
        stage.setMinHeight(370);
        stage.setScene(scene);
        stage.show();
        mainWindow = stage;
        Windows.setEffect(stage, DwmAPI.DWM_SYSTEMBACKDROP_TYPE.DWMSBT_TRANSIENTWINDOW);
        settingWindow = createSettingWindow();
    }

    @Override
    public void stop() {
        action.exit();
    }

    private void setAnchorPane() {
        AnchorPane.setLeftAnchor(leftPane, 0.0);
        AnchorPane.setLeftAnchor(progress, 0.0);
        AnchorPane.setLeftAnchor(rightPane, 300.0);

        AnchorPane.setRightAnchor(rightPane, 0.0);
        AnchorPane.setRightAnchor(progress, 0.0);
        AnchorPane.setRightAnchor(leftPane, 300.0);

        AnchorPane.setTopAnchor(leftPane, 0.0);
        AnchorPane.setTopAnchor(rightPane, 0.0);
        AnchorPane.setTopAnchor(progress, 360.0);

        AnchorPane.setBottomAnchor(leftPane, 10.0);
        AnchorPane.setBottomAnchor(rightPane, 10.0);
        AnchorPane.setBottomAnchor(progress, 0.0);
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
            changeLyricsPaneSize(largeSize, Resources.FontRes.yahei_large_font);
            button.setFont(Resources.FontRes.yahei_large_font);
            level = 3;
        } else if (((currentWidth > 700 && currentWidth <= 1000) &&
                (currentHeight > 430 && currentHeight <= 618)) &&
                level != 2) {
            rightPane.setPadding(new Insets(0, 20, 0, 0));
            view.setFitHeight(mediumSize);
            view.setFitWidth(mediumSize);
            changeLyricsPaneSize(mediumSize, Resources.FontRes.yahei_medium_font);
            button.setFont(Resources.FontRes.yahei_medium_font);
            level = 2;
        } else if ((currentWidth <= 700 || currentHeight <= 430) &&
                level != 1) {
            rightPane.setPadding(new Insets(0, 10, 0, 0));
            view.setFitHeight(smallSize);
            view.setFitWidth(smallSize);
            changeLyricsPaneSize(smallSize, Resources.FontRes.yahei_small_font);
            button.setFont(Resources.FontRes.yahei_small_font);
            level = 1;
        }
    }

    /**
     * 变更歌词界面大小
     * @param size 大小
     * @param font 字体
     */
    private void changeLyricsPaneSize(double size, Font font) {
        lyricsPane.setPrefWidth(size);
        lyricsPane.setPrefHeight(size);
        for (Node child : lyricsPane.getChildren()) {
            Label l = (Label) child;
            l.setFont(font);
        }
        Lyric.setCurrentSize(size);
        for (Lyric lyric : action.getLyricList()) {
            if (size == smallSize) lyric.setSmall();
            else if (size == mediumSize) lyric.setMedium();
            else if (size == largeSize) lyric.setLarge();
        }
    }

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
        ContextMenu menu = new ContextMenu();
        menu.setStyle(lightMenu);

        MenuItem openSetting = new MenuItem(Resources.StringRes.setting_item);
        CheckMenuItem openDark = new CheckMenuItem(Resources.StringRes.open_dark_mode_item);
        openDark.setSelected(false);
        Menu moreStylesMenu = new Menu(Resources.StringRes.more_styles_item);

        openSetting.setOnAction(actionEvent -> settingWindow.show());
        openDark.selectedProperty().addListener((
                observableValue,
                old, open) -> {
            Buttons.setLightOrDark(open, button);
            Windows.setLightOrDark(mainWindow, open);
            Lyric.setDark(open);
            for (Lyric lyric : action.getLyricList()) {
                if (lyric.getLabel() == lyricsPane.getChildren().get(1))
                    lyric.setHighLight(open);
                else lyric.setMode(open);
            }
            if (open) {
                menu.setStyle(darkMenu);
                for (MenuItem item : menu.getItems()) {
                    item.setStyle(darkItem);
                }
            }
            else {
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
                openDark,
                moreStylesMenu
        );
        return menu;
    }

    private Dialog<String> createSettingWindow() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(Resources.StringRes.setting_item);
        dialog.setWidth(400);
        dialog.setHeight(300);

        DialogPane pane = dialog.getDialogPane();
        StackPane context = new StackPane();

        context.setPrefWidth(400);
        context.setPrefHeight(200);
        context.getChildren().add(new Text("啥也没有"));

        pane.setContent(context);
        pane.getButtonTypes().addAll(ButtonType.OK);

        return dialog;
    }
}
