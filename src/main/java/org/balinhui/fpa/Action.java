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
import org.balinhui.fpa.info.SystemInfo;
import org.balinhui.fpa.nativeapis.Global;
import org.balinhui.fpa.ui.Lyric;
import org.balinhui.fpa.ui.LyricsPlayer;
import org.balinhui.fpa.ui.Taskbar;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.fpa.util.CoverColorExtractor;
import org.balinhui.fpa.util.Lyrics;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * 实现窗口操作与内部逻辑的联系
 */
public class Action {
    private static final Logger logger = LogManager.getLogger(Action.class);
    private static final Action action = new Action();
    private final Decoder decoder;
    private final Player player;
    private AudioInfo info;
    private final List<Lyric> lyricList = new ArrayList<>();
    private LyricsPlayer lPlayer;
    public static int playedSamples;
    public static volatile double currentTimeSeconds;

    public static Action initialize() {
        return action;
    }

    private Action() {
        //将这个充当初始化
        SystemInfo.read(
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("os.version")
        );
        logger.info("==========系统信息==========");
        logger.info("名称: {}", SystemInfo.systemInfo.name);
        logger.info("架构: {}", SystemInfo.systemInfo.arch);
        logger.info("版本: {}", SystemInfo.systemInfo.version);
        logger.info("==========列举完成==========");

        decoder = Decoder.getDecoder();
        player = Player.getPlayer();
        player.setOnFinished(args -> finish());
        player.setBeforePlaySample(this::flushProgress);
    }

    /**
     * “选择文件”按钮点击事件
     */
    public void clickChooseFileButton() {
        logger.info("按钮被点击");
        if (!CurrentStatus.is(CurrentStatus.Status.STOP)) {
            logger.info("播放当中，不许选择文件");
            return;
        }
        String[] paths = Global.chooseFiles();
        if (paths == null) {
            logger.info("没有文件");
            return;
        }
        inputPaths(paths);
    }

    /**
     * 暂停按钮点击事件
     */
    public void clickPauseButton() {
        if (CurrentStatus.is(CurrentStatus.Status.PLAYING)) {
            //暂停
            CurrentStatus.to(CurrentStatus.Status.PAUSE);
            lPlayer.pause();
            FPAScreen.pause.setGraphic(FPAScreen.playIcon);
            Taskbar.setPaused(true);//设置任务栏暂停状态
        } else if (CurrentStatus.is(CurrentStatus.Status.PAUSE)) {
            //播放
            CurrentStatus.to(CurrentStatus.Status.PLAYING);
            lPlayer.resume();
            FPAScreen.pause.setGraphic(FPAScreen.pauseIcon);
            Taskbar.setPaused(false);//恢复任务栏
        }
    }

    public void clickSettingApplyButton() {
        logger.info("设置中点击应用");
        FPAScreen.settingWindow.close();
    }

    /**
     * 通过拖入文件来播放
     * @param paths 文件绝对路径
     */
    public void inputPathsFromDropping(String[] paths) {
        logger.info("拖入文件");
        inputPaths(paths);
    }

    private void inputPaths(String[] paths) {
        if (paths.length == 1) processFile(paths[0]);
        else processFiles(paths);
    }

    private void processFile(String path) {
        logger.trace("单选文件");
        info = decoder.read(path);
        logger.trace("读取歌曲信息: {}", path);
        OutputInfo output = player.read(info);
        logger.info("取得输出信息：{}", output);

        play(output, null, null);
    }

    private void processFiles(String[] path) {
        logger.trace("多选文件");
        info = decoder.read(path);
        logger.trace("读取歌曲第一首信息: {}", path[0]);
        OutputInfo output = player.readForSameOut();
        logger.info("取得第一首输出信息：{}", output);


        play(output, progress -> {
            //在解码完成时调用，与歌曲播放结束不同步，用于提前解码下一首歌曲
            if (progress < path.length) {
                logger.trace("解码完一首，当前进度:第 {} 首", progress + 1);
                info = decoder.readOnly(path[progress]);
                logger.info("读取下首歌曲信息: {}", path[progress]);
            }
        }, process -> {
            //播放完一首后收到结束信息时调用，用于更新UI、时间轴和歌词
            playedSamples = 0;
            currentTimeSeconds = 0.0;
            Platform.runLater(() -> {
                setLyrics(info.metadata);
                if (info.cover != null) {
                    FPAScreen.view.setImage(new Image(new ByteArrayInputStream(info.cover)));
                    logger.trace("更新封面");
                    FPAScreen.progress.setStyle("-fx-accent: rgb(" + CoverColorExtractor.extractOneRGBColor(info.cover) + ");");
                }
            });
        });
    }

    private void play(OutputInfo output, AudioHandler.FinishEvent decoderEvent,
                                        AudioHandler.FinishEvent playerEvent) {
        //初始化时间
        playedSamples = 0;
        currentTimeSeconds = 0.0;
        logger.info("时间轴初始化");

        ArrayLoop.clear();
        decoder.setOutput(output);
        decoder.setOnFinished(decoderEvent);
        decoder.start();
        player.setOnPerSongFinished(playerEvent);
        //状态更新，同时为解码线程让出时间
        Platform.runLater(() -> {
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
        });

        if (!Taskbar.init(FPAScreen.mainWindow)) {
            //记录日志但是不做任何操作，因为没有在任务栏显示进度也不影响
            logger.error("任务栏进度条初始化失败");
        }

        player.start();
    }

    /**
     * 读取歌词，初始化歌词面板，启动歌词播放线程
     * @param metadata 歌曲元数据
     */
    private void setLyrics(Map<String, String> metadata) {
        TreeMap<Long, String> lyrics = Lyrics.read(metadata);
        logger.trace("获取歌词和时间轴");

        //清空上次剩余内容
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
        if (lPlayer != null) {
            logger.trace("停止歌词线程");
            lPlayer.stop();
        }
    }

    private void flushProgress(int samples) {
        playedSamples += samples;
        currentTimeSeconds = (double) playedSamples / info.sampleRate;
        double progress = currentTimeSeconds / info.durationSeconds;
        Platform.runLater(() -> {
            FPAScreen.progress.setProgress(progress);
            Taskbar.setProgress(progress);
        });
    }

    public List<Lyric> getLyricList() {
        return lyricList;
    }

    /**
     * 尝试将播放结束后的处理集中在此，不知是否有遗漏
     */
    public void finish() {
        logger.trace("(所有)歌曲结束");
        stopLyrics();
        Taskbar.release();
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
        logger.info("退出");
    }
}
