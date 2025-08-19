package org.balinhui.fpa;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.core.AudioHandler;
import org.balinhui.fpa.core.CurrentStatus;
import org.balinhui.fpa.core.Decoder;
import org.balinhui.fpa.core.Player;
import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;
import org.balinhui.fpa.ui.Lyric;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.fpa.util.Lyrics;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Action {
    private static final Logger logger = LogManager.getLogger(Action.class);
    private static final Action action = new Action();
    private final Decoder decoder;
    private final Player player;
    private AudioInfo info;
    private final List<Lyric> lyricList = new ArrayList<>();
    private ScheduledExecutorService executor;

    private native String[] chooseFiles();

    public static Action getInstance() {
        return action;
    }

    private Action() {
        System.loadLibrary("FileChooser");
        logger.trace("加载FileChooser库");
        decoder = Decoder.getDecoder();
        player = Player.getPlayer();
        player.setOnFinished(args -> finish());
        player.setBeforePlaySample(this::flashProgress);
    }

    /**
     * “选择文件”按钮点击事件
     */
    public void ClickButton() {
        logger.info("按钮被点击");
        String[] path = chooseFiles();
        if (path == null) {
            logger.info("没有文件");
            return;
        }
        if (path.length == 1) processFile(path[0]);
        else processFiles(path);
    }

    private void processFile(String path) {
        logger.trace("单选文件");
        info = decoder.read(path);
        logger.trace("读取歌曲信息: {}", path);
        OutputInfo output = player.read(info);
        logger.info("取得输出信息：{}", output);

        play(output, null);
    }

    private void processFiles(String[] path) {
        logger.trace("多选文件");
        info = decoder.read(path);
        logger.trace("读取歌曲第一首信息: {}", path[0]);
        OutputInfo output = player.readForSameOut(info);
        logger.info("取得第一首输出信息：{}", output);


        play(output, progress -> {
            if (progress < path.length) {
                logger.trace("解码完一首，当前进度:{}", progress);
                Platform.runLater(() -> {
                    info = decoder.readOnly(path[progress]);
                    logger.info("读取歌曲信息: {}", path[progress]);
                    setLyrics(info.metadata);
                    if (info.cover != null) {
                        Main.view.setImage(new Image(new ByteArrayInputStream(info.cover)));
                        logger.trace("更新封面");
                    }
                });
            }
        });
    }

    private void play(OutputInfo output, AudioHandler.FinishEvent event) {
        ArrayLoop.clear();
        decoder.setOutput(output);
        decoder.setOnFinished(event);
        decoder.start();
        //状态更新，同时为解码线程让出时间
        Main.rightPane.getChildren().remove(Main.button);
        logger.trace("移除按钮");
        Main.rightPane.getChildren().add(Main.lyricsPane);
        logger.trace("添加歌词面板");
        setLyrics(info.metadata);
        if (info.cover != null) {
            Main.view.setImage(new Image(new ByteArrayInputStream(info.cover)));
            logger.trace("更新封面");
        }
        player.start();
    }

    private void setLyrics(Map<String, String> metadata) {
        TreeMap<Long, String> lyrics = Lyrics.read(metadata);
        logger.trace("获取歌词和时间轴");

        Main.lyricsPane.getChildren().clear();
        lyricList.clear();
        int currentTime = 0;
        stopLyrics();
        executor = Executors.newSingleThreadScheduledExecutor();
        logger.trace("歌词初始化");

        for (Map.Entry<Long, String> entry : lyrics.entrySet()) {
            long delay = entry.getKey();
            String lyric = entry.getValue();
            int time = currentTime;
            lyricList.add(new Lyric(lyric, actionEvent -> lyricsPaneAdd(time + 1)));
            executor.schedule(() -> addLyrics(time), delay, TimeUnit.MILLISECONDS);
            currentTime++;
        }
        logger.info("歌词线程启动");
        Main.lyricsPane.getChildren().add(lyricList.getFirst().getLabel());
    }

    private void addLyrics(int currentLyricLine) {
        Platform.runLater(() -> {
            if (currentLyricLine > 1) {
                Main.lyricsPane.getChildren().remove(
                        lyricList.get(currentLyricLine - 2).getLabel()
                );
                lyricList.get(currentLyricLine - 1).playGo();
            } else if (currentLyricLine == 1) {
                lyricList.getFirst().playGo();
            }
            lyricList.get(currentLyricLine).playCome();
        });
    }

    private void lyricsPaneAdd(int currentLyricLine) {
        Platform.runLater(() -> {
            if (currentLyricLine < lyricList.size()) {
                Main.lyricsPane.getChildren().add(
                        lyricList.get(currentLyricLine).getLabel()
                );
            }
        });
    }

    public void stopLyrics() {
        logger.trace("停止歌词线程");
        if (executor != null)
            executor.shutdownNow();
    }

    public void flashProgress() {
        Platform.runLater(() ->
                Main.progress.setProgress(
                        decoder.getCurrentTimeSeconds() / info.durationSeconds
                )
        );
    }

    public List<Lyric> getLyricList() {
        return lyricList;
    }

    /**
     * 尝试将播放结束后的处理集中在此，不知是否有遗漏
     */
    public void finish() {
        logger.trace("歌曲结束");
        stopLyrics();
        Platform.runLater(() -> {
            Main.rightPane.getChildren().remove(Main.lyricsPane);
            Main.rightPane.getChildren().add(Main.button);
            Main.view.setImage(Resources.ImageRes.cover);
            Main.progress.setProgress(-1);
        });
    }

    /**
     * 尝试将程序退出时处理集中在此，不知是否有遗漏
     */
    public void exit() {
        CurrentStatus.to(CurrentStatus.Status.STOP);
        //player.setFinished(player::terminate);  终止线程与初始化线程不统一，调用失败
        stopLyrics();
    }
}
