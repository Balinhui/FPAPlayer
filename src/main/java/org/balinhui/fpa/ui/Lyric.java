package org.balinhui.fpa.ui;

import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.balinhui.fpa.Resources;
import org.balinhui.fpa.util.Config;

public class Lyric {
    private final Label lyric;
    private final long time;
    private ParallelTransition come;
    private ParallelTransition go;
    private final GaussianBlur blur = new GaussianBlur();
    private static int currentSize = 1; // 1->small, 2->medium, 3->large
    private static boolean dark = false;
    private final EventHandler<ActionEvent> eventHandler;
    private static final double DEFORMATION_SIZE = 1.005;

    private static final Paint GRAY_WHITE = Color.rgb(190, 190, 190);
    private static final Paint GRAY_DARK = Color.rgb(65, 65, 65);


    public Lyric(String context, long time, EventHandler<ActionEvent> eventHandler) {
        this.lyric = new Label(context);
        this.lyric.setAlignment(Pos.CENTER);
        switch (Config.location()) {
            case "left" -> this.lyric.setTextAlignment(TextAlignment.LEFT);
            case "right" -> this.lyric.setTextAlignment(TextAlignment.RIGHT);
            default -> this.lyric.setTextAlignment(TextAlignment.CENTER);
        }
        this.lyric.setWrapText(true);
        if (dark) {
            this.lyric.setTextFill(GRAY_WHITE);
        } else {
            this.lyric.setTextFill(GRAY_DARK);
        }
        this.eventHandler = eventHandler;
        switch (currentSize) {
            case 1 -> setSmall();
            case 2 -> setMedium();
            case 3 -> setLarge();
            default -> throw new RuntimeException("Size does not exist");
        }
        this.blur.setRadius(2.5);
        this.lyric.setEffect(blur);
        this.time = time;
    }

    public long getTime() {
        return this.time;
    }

    public Label getLabel() {
        return this.lyric;
    }

    /**
     * 播放动画，从原位置(面板下方)移至中央
     */
    public void playCome() {
        this.come.play();
        if (dark) {
            this.lyric.setTextFill(Color.WHITE);
        } else {
            this.lyric.setTextFill(Color.BLACK);
        }
        this.blur.setRadius(0.0);
    }

    /**
     * 播放动画，从面板中央移至上方
     */
    public void playGo() {
        this.go.play();
        if (dark) {
            this.lyric.setTextFill(GRAY_WHITE);
        } else {
            this.lyric.setTextFill(GRAY_DARK);
        }
        this.blur.setRadius(2.5);
    }

    public void setLarge() {
        this.lyric.setFont(Resources.FontRes.noto_large_font);
        setAnimation(-155);
    }

    public void setMedium() {
        this.lyric.setFont(Resources.FontRes.noto_medium_font);
        setAnimation(-100);
    }

    public void setSmall() {
        this.lyric.setFont(Resources.FontRes.noto_small_font);
        setAnimation(-70);
    }

    private void setAnimation(double toY) {
        TranslateTransition translateCome = new TranslateTransition(Duration.millis(200), lyric);
        translateCome.setFromY(0);
        translateCome.setToY(toY);
        ScaleTransition scaleCome = new ScaleTransition(Duration.millis(200), lyric);
        scaleCome.setFromX(1);
        scaleCome.setFromY(1);
        scaleCome.setToX(DEFORMATION_SIZE);
        scaleCome.setToY(DEFORMATION_SIZE);
        this.come = new ParallelTransition(translateCome, scaleCome);
        this.come.setOnFinished(eventHandler);
        TranslateTransition translateGo = new TranslateTransition(Duration.millis(200), lyric);
        translateGo.setFromY(toY);
        translateGo.setToY(2 * toY);
        ScaleTransition scaleGo = new ScaleTransition(Duration.millis(200), lyric);
        scaleGo.setFromX(DEFORMATION_SIZE);
        scaleGo.setFromY(DEFORMATION_SIZE);
        scaleGo.setToX(1);
        scaleGo.setToY(1);
        this.go = new ParallelTransition(translateGo, scaleGo);
    }

    public void topLocation() {
        switch (currentSize) {
            case 1 -> topTrans(-70);
            case 2 -> topTrans(-100);
            case 3 -> topTrans(-155);
        }
    }

    private void topTrans(double y) {
        TranslateTransition tra = new TranslateTransition(Duration.ONE, lyric);
        tra.setFromY(2 * y);
        tra.setToY(2 * y);
        ScaleTransition sca = new ScaleTransition(Duration.ONE, lyric);
        sca.setFromX(1);
        sca.setFromY(1);
        sca.setToX(1);
        sca.setToY(1);
        ParallelTransition par = new ParallelTransition(tra, sca);
        par.play();
    }

    public void mediumLocation() {
        switch (currentSize) {
            case 1 -> mediumTrans(-70);
            case 2 -> mediumTrans(-100);
            case 3 -> mediumTrans(-155);
        }
    }

    private void mediumTrans(double y) {
        TranslateTransition tra = new TranslateTransition(Duration.ONE, lyric);
        tra.setFromY(y);
        tra.setToY(y);
        ScaleTransition sca = new ScaleTransition(Duration.ONE, lyric);
        sca.setFromX(DEFORMATION_SIZE);
        sca.setFromY(DEFORMATION_SIZE);
        sca.setToX(DEFORMATION_SIZE);
        sca.setToY(DEFORMATION_SIZE);
        ParallelTransition par = new ParallelTransition(tra, sca);
        par.play();
    }

    public void setMode(boolean flag) {
        if (flag) {
            this.lyric.setTextFill(GRAY_WHITE);
        } else {
            this.lyric.setTextFill(GRAY_DARK);
        }
    }

    public void setModeForHighLight(boolean flag) {
        if (flag) {
            this.lyric.setTextFill(Color.WHITE);
        } else {
            this.lyric.setTextFill(Color.BLACK);
        }
    }

    public static void setCurrentSize(double size) {
        if (size == 220) currentSize = 1;
        else if (size == 300) currentSize = 2;
        else if (size == 450) currentSize = 3;
    }

    public static void setDark(boolean flag) {
        dark = flag;
    }
}
