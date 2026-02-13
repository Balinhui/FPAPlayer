package org.balinhui.fpa.ui;

import javafx.scene.control.Button;
import org.jetbrains.annotations.NotNull;

public class Buttons {

    /**
     * 设置按钮的颜色，暗黑模式
     * @param flag 是否为暗黑模式
     * @param color 颜色，目前只有白色（深色模式中为黑色）和蓝色
     * @param buttons 需要设置的按钮
     */
    public static void setLightOrDark(boolean flag, Color color, @NotNull Button... buttons) {
        if (flag) setDark(color, buttons);
        else setLight(color, buttons);
    }

    /**
     * 将按钮按照颜色设置为白色模式
     * @param color 需要的颜色
     * @param buttons 需要设置的按钮
     */
    private static void setLight(Color color, @NotNull Button... buttons) {
        String background = "#ffffff";
        String text = "#000000";
        String border = "#cccccc";
        String enteredBackground = "#f6f6f6";
        String pressedBackground = "#f6f6f6";
        String pressedText = "#595b5d";
        if (color == Color.WHITE) {
            background = "#ffffff";
            text = "#000000";
            border = "#cccccc";
            enteredBackground = "#f6f6f6";
            pressedBackground = "#f6f6f6";
            pressedText = "#595b5d";
        } else if (color == Color.BLUE) {
            background = "#0067c0";
            text = "#ffffff";
            border = "#003e73";
            enteredBackground = "#1975c5";
            pressedBackground = "#3183ca";
            pressedText = "#c2daef";
        }
        setButtons(background, text, border, enteredBackground, pressedBackground, pressedText, buttons);
    }

    /**
     * 将按钮按照颜色设置为暗黑模式
     * @param color 需要的颜色
     * @param buttons 需要设置的按钮
     */
    private static void setDark(Color color, @NotNull Button... buttons) {
        String background = "#343536";
        String text = "#ffffff";
        String border = "#454545";
        String enteredBackground = "#3c3c3c";
        String pressedBackground = "#2f3030";
        String pressedText = "#b4b5b5";
        if (color == Color.WHITE) {
            background = "#343536";
            text = "#ffffff";
            border = "#454545";
            enteredBackground = "#3c3c3c";
            pressedBackground = "#2f3030";
            pressedText = "#b4b5b5";
        } else if (color == Color.BLUE) {
            background = "#4cc2ff";
            text = "#000000";
            border = "#42a7dc";
            enteredBackground = "#99ebff";
            pressedBackground = "#0091f8";
            pressedText = "#00487c";
        }
        setButtons(background, text, border, enteredBackground, pressedBackground, pressedText, buttons);
    }

    /**
     * 按钮的设置项
     * @param background 一般背景颜色
     * @param text 一般文本颜色
     * @param border 边框颜色
     * @param enteredBackground 鼠标进入时的背景颜色
     * @param pressedBackground 鼠标点击时的背景颜色
     * @param pressedText 鼠标点击时的文本颜色
     * @param buttons 需要设置的按钮
     */
    private static void setButtons(String background, String text, String border,
                                   String enteredBackground, String pressedBackground, String pressedText,
                                   @NotNull Button[] buttons) {
        for (Button button : buttons) {
            button.setStyle("-fx-background-color: "+ background +";" +
                    "-fx-text-base-color: " + text + ";" +
                    "-fx-background-radius: 5.0;" +
                    "-fx-border-color: " + border + ";" +
                    "-fx-border-radius: 5.0;" +
                    "-fx-border-width: 1 1 2 1;");
            button.setOnMouseEntered( mouseEvent -> button.setStyle(
                    "-fx-background-color: " + enteredBackground +";" +
                            "-fx-text-base-color: " + text + ";" +
                            "-fx-background-radius: 5.0;" +
                            "-fx-border-color: " + border + ";" +
                            "-fx-border-radius: 5.0;" +
                            "-fx-border-width: 1 1 2 1;"));
            button.setOnMouseExited(mouseEvent -> button.setStyle(
                    "-fx-background-color: " + background + ";" +
                            "-fx-text-base-color: " + text + ";" +
                            "-fx-background-radius: 5.0;" +
                            "-fx-border-color: " + border + ";" +
                            "-fx-border-radius: 5.0; " +
                            "-fx-border-width: 1 1 2 1;"));
            button.setOnMousePressed(mouseEvent -> button.setStyle(
                    "-fx-background-color: " + pressedBackground + ";" +
                            "-fx-text-base-color: " + pressedText + ";" +
                            "-fx-background-radius: 5.0; " +
                            "-fx-border-color: " + border + ";" +
                            "-fx-border-radius: 5.0;" +
                            "-fx-border-width: 1;"));
            button.setOnMouseReleased(mouseEvent -> button.setStyle(
                    "-fx-background-color: " + background + ";" +
                            "-fx-text-base-color: " + text + ";" +
                            "-fx-background-radius: 5.0;" +
                            "-fx-border-color: " + border + ";" +
                            "-fx-border-radius: 5.0;" +
                            "-fx-border-width: 1 1 2 1;"));
        }
    }

    /**
     * 给Buttons使用的颜色，设置按钮的颜色
     */
    public enum Color {
        //白色，暗黑模式下为黑色
        WHITE,
        //蓝色
        BLUE
    }
}
