package org.balinhui.fpa;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import org.balinhui.fpa.core.Decoder;
import org.balinhui.fpa.core.Player;
import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;

import java.io.ByteArrayInputStream;

public class Action {
    private static final Action action = new Action();
    private final Decoder decoder;
    private final Player player;
    private long playedSamples = 0;
    private AudioInfo info;

    private native String getPath();

    public static Action getInstance() {
        return action;
    }

    private Action() {
        System.loadLibrary("FileChooser");
        decoder = Decoder.getDecoder();
        player = Player.getPlayer();
        player.setFinished(this::finish);
        player.setBeforePlaySample(this::flashProgress);
    }

    public void ClickButton(ActionEvent e) {
        String path = getPath();
        if (path == null) return;
        info = decoder.read(path);
        OutputInfo output = player.read(info);
        decoder.setOutput(output);
        Thread decode = new Thread(decoder);
        decode.setName("decode");
        decode.start();
        //状态更新，同时为解码线程让出时间
        Main.right.getChildren().remove(Main.button);
        Main.right.getChildren().add(Main.text);
        Main.view.setImage(new Image(new ByteArrayInputStream(info.cover)));

        Thread play = new Thread(player);
        play.setName("play");
        play.start();
    }

    public void finish() {
        Platform.runLater(() -> {
            Main.right.getChildren().remove(Main.text);
            Main.right.getChildren().add(Main.button);
            Main.view.setImage(new Image("cover.png"));
            Main.progress.setProgress(-1);
            playedSamples = 0;
            player.stop();
        });
    }

    public void flashProgress(int nb_samples) {
        Platform.runLater(() -> {
            playedSamples += nb_samples;
            float currentTime = (float) playedSamples / info.sampleRate;
            Main.progress.setProgress(currentTime / info.durationSeconds);
        });
    }
}
