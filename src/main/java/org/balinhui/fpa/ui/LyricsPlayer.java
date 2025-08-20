package org.balinhui.fpa.ui;

import javafx.application.Platform;
import org.balinhui.fpa.Action;
import org.balinhui.fpa.FPAScreen;

import java.util.List;

public class LyricsPlayer {
    private final List<Lyric> lyrics;
    private int currentIndex = 0;
    private long startTime = 0;
    private long pauseTime = 0;
    private volatile boolean playing = false;

    public LyricsPlayer(List<Lyric> lyrics) {
        this.lyrics = lyrics;
    }

    public void play() {
        if (playing) return;
        resume();
    }

    public void pause() {
        if (!playing) return;
        playing = false;
        pauseTime = System.currentTimeMillis();
    }

    public void resume() {
        playing = true;
        if (pauseTime == 0)
            startTime = System.currentTimeMillis();
        else {
            startTime = (long) (System.currentTimeMillis() - Action.currentTimeSeconds * 1000);
        }
        Thread thread = new Thread(this::loop);
        thread.setName("Lyrics Thread");
        thread.start();
    }

    public void stop() {
        playing = false;
    }

    private long getCurrentPosition() {
        if (!playing) return pauseTime;
        return System.currentTimeMillis() - startTime;
    }

    private void loop() {
        while (playing && currentIndex < lyrics.size()) {
            long position = getCurrentPosition();
            Lyric lyric = lyrics.get(currentIndex);
            if (position >= lyric.getTime()) {
                addLyrics(currentIndex);
                currentIndex++;
            }
            try {
                Thread.sleep(10); // 每 10ms 检查一次
            } catch (InterruptedException ignored) {}
        }
    }

    private void addLyrics(int currentLyricLine) {
        Platform.runLater(() -> {
            if (currentLyricLine > 1) {
                FPAScreen.lyricsPane.getChildren().remove(
                        lyrics.get(currentLyricLine - 2).getLabel()
                );
                lyrics.get(currentLyricLine - 1).playGo();
            } else if (currentLyricLine == 1) {
                lyrics.getFirst().playGo();
            }
            lyrics.get(currentLyricLine).playCome();
        });
    }
}
