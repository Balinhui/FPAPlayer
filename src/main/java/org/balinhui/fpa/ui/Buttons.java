package org.balinhui.fpa.ui;

import javafx.scene.control.Button;
import org.jetbrains.annotations.NotNull;

public class Buttons {
    public static void setLight(@NotNull Button... buttons) {
        for (Button button : buttons) {
            button.setStyle("-fx-background-color: #ffffff;" +
                    "-fx-text-base-color: #000000;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #cccccc; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1 1 2 1;");
            button.setOnMouseEntered( mouseEvent -> button.setStyle("-fx-background-color: #f6f6f6;" +
                    "-fx-text-base-color: #000000;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #cccccc; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1 1 2 1;"));
            button.setOnMouseExited(mouseEvent -> button.setStyle("-fx-background-color: #ffffff;" +
                    "-fx-text-base-color: #000000;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #cccccc; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1 1 2 1;"));
            button.setOnMousePressed(mouseEvent -> button.setStyle("-fx-background-color: #f6f6f6;" +
                    "-fx-text-base-color: #595b5d;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #cccccc; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1;"));
            button.setOnMouseReleased(mouseEvent -> button.setStyle("-fx-background-color: #ffffff;" +
                    "-fx-text-base-color: #000000;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #cccccc; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1 1 2 1;"));
        }
    }

    public static void setDark(@NotNull Button... buttons) {
        for (Button button : buttons) {
            button.setStyle("-fx-background-color: #343536;" +
                    "-fx-text-base-color: #ffffff;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #454545; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1 1 2 1;");
            button.setOnMouseEntered( mouseEvent -> button.setStyle("-fx-background-color: #3c3c3c;" +
                    "-fx-text-base-color: #ffffff;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #454545; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1 1 2 1;"));
            button.setOnMouseExited(mouseEvent -> button.setStyle("-fx-background-color: #343536;" +
                    "-fx-text-base-color: #ffffff;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #454545; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1 1 2 1;"));
            button.setOnMousePressed(mouseEvent -> button.setStyle("-fx-background-color: #2f3030;" +
                    "-fx-text-base-color: #b4b5b5;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #454545; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1;"));
            button.setOnMouseReleased(mouseEvent -> button.setStyle("-fx-background-color: #343536;" +
                    "-fx-text-base-color: #ffffff;" +
                    "-fx-background-radius: 5.0; " +
                    "-fx-border-color: #454545; " +
                    "-fx-border-radius: 5.0; " +
                    "-fx-border-width: 1 1 2 1;"));
        }
    }
}
