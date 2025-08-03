package org.balinhui.fpa;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.balinhui.fpa.core.CurrentStatus;
import org.balinhui.fpa.ui.Buttons;
import org.balinhui.fpa.ui.DwmAPI;
import org.balinhui.fpa.ui.Windows;

public class Main extends Application {
    public static final ImageView view = new ImageView("cover.png");
    public static final Button button = new Button("选择文件");
    public static final Label text = new Label("还在施工");
    public static final HBox left = new HBox();
    public static final VBox right = new VBox();
    public static final ProgressBar progress = new ProgressBar();
    public static double currentWidth;
    public static double currentHeight;

    private final Font largeFont = new Font("Microsoft YaHei", 30);
    private final Font mediumFont = new Font("Microsoft YaHei", 20);
    private final Font smallFont = new Font("Microsoft YaHei", 15);

    private Action action;

    @Override
    public void init() throws Exception {
        action = Action.getInstance();
    }

    @Override
    public void start(Stage stage) throws Exception {

        left.setAlignment(Pos.CENTER);
        view.setFitWidth(220);
        view.setFitHeight(220);
        left.getChildren().add(view);

        right.setAlignment(Pos.CENTER);
        button.setFont(new Font("Microsoft YaHei", 15));
        button.setOnAction(action::ClickButton);
        Buttons.setLight(button);
        right.getChildren().add(button);

        AnchorPane root = new AnchorPane();
        root.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        root.getChildren().addAll(left, right, progress);

        AnchorPane.setLeftAnchor(left, 0.0);
        AnchorPane.setLeftAnchor(progress, 0.0);
        AnchorPane.setLeftAnchor(right, 300.0);

        AnchorPane.setRightAnchor(right, 0.0);
        AnchorPane.setRightAnchor(progress, 0.0);
        AnchorPane.setRightAnchor(left, 300.0);

        AnchorPane.setTopAnchor(left, 0.0);
        AnchorPane.setTopAnchor(right, 0.0);
        AnchorPane.setTopAnchor(progress, 360.0);

        AnchorPane.setBottomAnchor(left, 10.0);
        AnchorPane.setBottomAnchor(right, 10.0);
        AnchorPane.setBottomAnchor(progress, 0.0);

        stage.widthProperty().addListener((observableValue, number, t1) -> {
            currentWidth = t1.doubleValue();
            changeSize();
            double d = t1.doubleValue() / 2.0;
            AnchorPane.setRightAnchor(left, d);
            AnchorPane.setLeftAnchor(right, d);
        });
        stage.heightProperty().addListener((observableValue, number, t1) -> {
            currentHeight = t1.doubleValue();
            changeSize();
            double d = t1.doubleValue() - 47;
            AnchorPane.setTopAnchor(progress, d);
        });


        Scene scene = new Scene(root, 600, 370);
        scene.setFill(Color.TRANSPARENT);


        stage.initStyle(StageStyle.UNIFIED);
        stage.setTitle("FPA");
        stage.setMinWidth(600);
        stage.setMinHeight(370);
        stage.setScene(scene);
        stage.show();
        Windows.setEffect(stage, DwmAPI.DWM_SYSTEMBACKDROP_TYPE.DWMSBT_TRANSIENTWINDOW);
    }

    @Override
    public void stop() throws Exception {
        CurrentStatus.currentStatus = CurrentStatus.Status.STOP;
    }

    private void changeSize() {
        if (currentWidth > 900 && currentHeight > 550) {
            view.setFitHeight(400);
            view.setFitWidth(400);
            button.setFont(largeFont);
        } else if (currentWidth > 700 && currentHeight > 430) {
            view.setFitHeight(300);
            view.setFitWidth(300);
            button.setFont(mediumFont);
        } else {
            view.setFitHeight(220);
            view.setFitWidth(220);
            button.setFont(smallFont);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
