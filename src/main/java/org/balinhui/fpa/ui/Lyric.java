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

public class Lyric {
    private final Label lyric;
    private ParallelTransition come;
    private ParallelTransition go;
    private final GaussianBlur blur = new GaussianBlur();
    private static int currentSize = 1; // 1->small, 2->medium, 3->large
    private static boolean dark = false;
    private final EventHandler<ActionEvent> eventHandler;

    private static final Paint GRAY_WHITE = Color.rgb(190, 190, 190);
    private static final Paint GRAY_DARK = Color.rgb(60, 63, 65);


    public Lyric(String context, EventHandler<ActionEvent> eventHandler) {
        this.lyric = new Label(context);
        this.lyric.setAlignment(Pos.CENTER);
        this.lyric.setTextAlignment(TextAlignment.CENTER);
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
        this.lyric.setFont(Resources.FontRes.large_font);
        setAnimation(-155);
    }

    public void setMedium() {
        this.lyric.setFont(Resources.FontRes.medium_font);
        setAnimation(-100);
    }

    public void setSmall() {
        this.lyric.setFont(Resources.FontRes.small_font);
        setAnimation(-70);
    }

    private void setAnimation(double toY) {
        TranslateTransition translateCome = new TranslateTransition(Duration.millis(200), lyric);
        translateCome.setFromY(0);
        translateCome.setToY(toY);
        ScaleTransition scaleCome = new ScaleTransition(Duration.millis(200), lyric);
        scaleCome.setFromX(1);
        scaleCome.setFromY(1);
        scaleCome.setToX(1.02);
        scaleCome.setToY(1.02);
        this.come = new ParallelTransition(translateCome, scaleCome);
        this.come.setOnFinished(eventHandler);
        TranslateTransition translateGo = new TranslateTransition(Duration.millis(200), lyric);
        translateGo.setFromY(toY);
        translateGo.setToY(2 * toY);
        ScaleTransition scaleGo = new ScaleTransition(Duration.millis(200), lyric);
        scaleGo.setFromX(1.02);
        scaleGo.setFromY(1.02);
        scaleGo.setToX(1);
        scaleGo.setToY(1);
        this.go = new ParallelTransition(translateGo, scaleGo);
    }

    public void setMode(boolean flag) {
        if (flag) {
            this.lyric.setTextFill(GRAY_WHITE);
        } else {
            this.lyric.setTextFill(GRAY_DARK);
        }
    }

    public void setHighLight(boolean flag) {
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
