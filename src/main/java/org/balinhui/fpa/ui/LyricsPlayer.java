package org.balinhui.fpa.ui;

import javafx.application.Platform;
import org.balinhui.fpa.Action;
import org.balinhui.fpa.FPAScreen;

import java.util.List;

public class LyricsPlayer {
    private final List<Lyric> lyrics;

    /**
     * 代表当前在歌词面板下方的歌词索引
     */
    private int currentIndex = 0;
    private long startTime = 0;
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
    }

    public void resume() {
        playing = true;
        Thread thread = new Thread(this::loop);
        thread.setName("Lyrics Thread");
        startTime = (long) (System.currentTimeMillis() - Action.currentTimeSeconds * 1000);
        thread.start();
    }

    public void stop() {
        playing = false;
    }

    private long getCurrentPosition() {
        return System.currentTimeMillis() - startTime;
    }

    private void loop() {
        while (playing && currentIndex < lyrics.size()) {
            long position = getCurrentPosition();
            Lyric lyric = lyrics.get(currentIndex);
            if (position >= lyric.getTime()) {
                pushLyrics(currentIndex);
                currentIndex++;
            }
            try {
                Thread.sleep(10); // 每 10ms 检查一次
            } catch (InterruptedException ignored) {}
        }
    }

    private void pushLyrics(int currentLyricLine) {
        Platform.runLater(() -> {
            //排查多余的歌词，不知是什么情况而产生，暂时决定直接移除
            if (FPAScreen.lyricsPane.getChildren().size() > 3) {
                int count = FPAScreen.lyricsPane.getChildren().size() - 3;//多余的歌词
                for (int i = 0; i < count; i++) {
                    FPAScreen.lyricsPane.getChildren().remove(i);//移除前面的的，留下3个
                }
            }

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
