package org.balinhui.fpa;

import javafx.application.Application;

import java.io.File;

public class Launcher {

    public static void main(String[] args) {
        File log = new File("logs\\app.log");
        if (log.exists() && log.length() > 600000) {
            if (!log.delete()) {
                System.exit(0);
            }
        }
        Application.launch(FPAScreen.class, args);
    }
}
