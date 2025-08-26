package org.balinhui.fpa;

import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
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
import org.balinhui.fpa.ui.LyricsPlayer;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.fpa.util.Lyrics;
import org.balinhui.fpa.util.CoverColorExtractor;

import java.io.ByteArrayInputStream;
import java.util.*;

public class Action {
    private static final Logger logger = LogManager.getLogger(Action.class);
    private static final Action action = new Action();
    private final Decoder decoder;
    private final Player player;
    private AudioInfo info;
    private final List<Lyric> lyricList = new ArrayList<>();
    private LyricsPlayer lPlayer;
    public static int playedSamples;//归零在Decoder.java
    public static volatile double currentTimeSeconds;//同上

    private native String[] chooseFiles();

    public static Action getInstance() {
        return action;
    }

    private Action() {
        System.loadLibrary("file_chooser");
        logger.trace("加载file_chooser库");
        decoder = Decoder.getDecoder();
        player = Player.getPlayer();
        player.setOnFinished(args -> finish());
        player.setBeforePlaySample(this::flashProgress);
    }

    /**
     * “选择文件”按钮点击事件
     */
    public void clickChooseFileButton() {
        logger.info("按钮被点击");
        String[] path = chooseFiles();
        if (path == null) {
            logger.info("没有文件");
            return;
        }
        if (path.length == 1) processFile(path[0]);
        else processFiles(path);
    }

    public void clickPauseButton() {
        if (CurrentStatus.is(CurrentStatus.Status.PLAYING)) {
            CurrentStatus.to(CurrentStatus.Status.PAUSE);
            lPlayer.pause();
            FPAScreen.pause.setGraphic(FPAScreen.playIcon);
            logger.info("暂停");
        } else if (CurrentStatus.is(CurrentStatus.Status.PAUSE)) {
            CurrentStatus.to(CurrentStatus.Status.PLAYING);
            lPlayer.resume();
            FPAScreen.pause.setGraphic(FPAScreen.pauseIcon);
            logger.info("播放");
        }
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
        OutputInfo output = player.readForSameOut();
        logger.info("取得第一首输出信息：{}", output);


        play(output, progress -> {
            if (progress < path.length) {
                logger.trace("解码完一首，当前进度:{}", progress);
                Platform.runLater(() -> {
                    info = decoder.readOnly(path[progress]);
                    logger.info("读取歌曲信息: {}", path[progress]);
                    setLyrics(info.metadata);
                    if (info.cover != null) {
                        FPAScreen.view.setImage(new Image(new ByteArrayInputStream(info.cover)));
                        logger.trace("更新封面");
                        FPAScreen.progress.setStyle("-fx-accent: rgb(" + CoverColorExtractor.extractOneRGBColor(info.cover) + ");");
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
        FPAScreen.rightPane.getChildren().remove(FPAScreen.button);
        logger.trace("移除按钮");
        FPAScreen.rightPane.getChildren().add(FPAScreen.lyricsPane);
        logger.trace("添加歌词面板");
        setLyrics(info.metadata);
        if (info.cover != null) {
            FPAScreen.view.setImage(new Image(new ByteArrayInputStream(info.cover)));
            logger.trace("更新封面");
            FPAScreen.progress.setStyle("-fx-accent: rgb(" + CoverColorExtractor.extractOneRGBColor(info.cover) + ");");
        }
        FPAScreen.leftPane.getChildren().add(FPAScreen.control);
        player.start();
    }

    private void setLyrics(Map<String, String> metadata) {
        TreeMap<Long, String> lyrics = Lyrics.read(metadata);
        logger.trace("获取歌词和时间轴");

        FPAScreen.lyricsPane.getChildren().clear();
        lyricList.clear();
        int currentLine = 0;
        stopLyrics();
        logger.trace("歌词初始化");

        for (Map.Entry<Long, String> entry : lyrics.entrySet()) {
            long delay = entry.getKey();
            String lyric = entry.getValue();
            int line = currentLine;
            lyricList.add(new Lyric(lyric, delay, actionEvent -> lyricsPaneAdd(line + 1)));
            currentLine++;
        }
        FPAScreen.lyricsPane.getChildren().add(lyricList.getFirst().getLabel());
        lPlayer = new LyricsPlayer(lyricList);
        lPlayer.play();
        logger.info("歌词线程启动");
    }

    private void lyricsPaneAdd(int currentLyricLine) {
        Platform.runLater(() -> {
            if (currentLyricLine < lyricList.size()) {
                FPAScreen.lyricsPane.getChildren().add(
                        lyricList.get(currentLyricLine).getLabel()
                );
            }
        });
    }

    private void stopLyrics() {
        logger.trace("停止歌词线程");
        if (lPlayer != null)
            lPlayer.stop();
    }

    public void settingResult(Optional<FPAScreen.SettingResult> result) {
        result.ifPresentOrElse(settingResult ->
            logger.info("选择了: {}", settingResult), () ->
            logger.warn("非法结果")
        );
    }

    private void flashProgress(int samples) {
        playedSamples += samples;
        currentTimeSeconds = (double) playedSamples / info.sampleRate;
        Platform.runLater(() ->
                FPAScreen.progress.setProgress(
                        currentTimeSeconds / info.durationSeconds
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
            FPAScreen.rightPane.getChildren().remove(FPAScreen.lyricsPane);
            FPAScreen.rightPane.getChildren().add(FPAScreen.button);
            FPAScreen.view.setImage(Resources.ImageRes.cover);
            FPAScreen.progress.setStyle("-fx-accent: gray");
            FPAScreen.progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            FPAScreen.leftPane.getChildren().remove(FPAScreen.control);
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
