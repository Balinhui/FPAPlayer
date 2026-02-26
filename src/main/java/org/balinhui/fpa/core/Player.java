package org.balinhui.fpa.core;

import com.portaudio.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.FPAScreen;
import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;
import org.balinhui.fpa.nativeapis.Global;
import org.balinhui.fpa.nativeapis.MessageFlags;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.fpa.util.AudioUtil;
import org.balinhui.fpa.util.Config;
import org.balinhui.fpa.util.Win32;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.portaudio.PortAudio.*;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLT;

public class Player implements Runnable, AudioHandler {
    private static final Logger logger = LogManager.getLogger(Player.class);
    private final Buffer buffer = new Buffer();
    private static final Player player = new Player();
    private int cId;//Current device id
    private int cMaxOutputChannels;//current
    private int cMaxOutputSampleRate;//current
    private BlockingStream stream;
    private FinishEvent event;//当播放循环结束后会调用
    private FinishEvent finishPerSong;
    private PlaySample start;//播放循环期间每一帧调用一次
    private final ExecutorService singleThread;//player的唯一线程，一切与portaudio有关的操作都将在这里进行

    public static Player getPlayer() {
        return player;
    }

    @Override
    public void setOnFinished(FinishEvent event) {
        this.event = event;
    }

    public void setOnPerSongFinished(FinishEvent event) {
        this.finishPerSong = event;
    }

    public void setBeforePlaySample(PlaySample start) {
        this.start = start;
    }

    /**
     * 初始化Player
     */
    private Player() {
        //初始化Play的线程
        ThreadFactory factory = r -> new Thread(r, "Play Thread");
        singleThread = Executors.newSingleThreadExecutor(factory);

        singleThread.submit(() -> {
            //初始化PortAudio
            initialize();

            //获取默认输出设备信息
            int id = getDefaultOutputDevice();
            if (Config.api().equals("WASAPI"))
                id = getHostApiInfo(hostApiTypeIdToHostApiIndex(HOST_API_TYPE_WASAPI)).defaultOutputDevice;
            this.cId = id;
            DeviceInfo deviceInfo = getDeviceInfo(id);
            logger.info("默认 {} 输出设备为: {}",Config.api() , deviceInfo.name);

            //取得当前的设备的最大输出声道数和采样率
            getDeviceChannelsAndSampleRateInfo(id, deviceInfo);

            logger.info("设备最大支持采样率: {} Hz", cMaxOutputSampleRate);
        });
    }

    private void getDeviceChannelsAndSampleRateInfo(int id, DeviceInfo deviceInfo) {
        this.cMaxOutputChannels = deviceInfo.maxOutputChannels;
        StreamParameters test = new StreamParameters();
        test.device = id;
        test.channelCount = this.cMaxOutputChannels;
        test.suggestedLatency = deviceInfo.defaultLowOutputLatency;
        int[] sampleRatesToTest = {384000, 192000, 96000, 88200, 48000, 44100, 8000};
        int maxOutputSampleRate = sampleRatesToTest[sampleRatesToTest.length - 1];
        for (int v : sampleRatesToTest) {
            if (isFormatSupported(null, test, v) == 0) {
                maxOutputSampleRate = v;
                break;
            }
        }
        this.cMaxOutputSampleRate = maxOutputSampleRate;
    }

    /**
     * 刷新默认输出设备，如果检测到设备变化，则重新读取
     */
    private void refreshDevice() {
        int id = getDefaultOutputDevice();
        if (Config.api().equals("WASAPI"))
            id = getHostApiInfo(hostApiTypeIdToHostApiIndex(HOST_API_TYPE_WASAPI)).defaultOutputDevice;
        if (id != cId) {
            logger.info("检测到输出设备更改，由id: {} -> id: {}", cId, id);
            this.cId = id;
            DeviceInfo deviceInfo = getDeviceInfo(id);
            logger.trace("取得当前输出设备: {}", deviceInfo.name);

            getDeviceChannelsAndSampleRateInfo(id, deviceInfo);
        }
    }

    /**
     * 分析当前歌曲，决定是否需要重采样，同时打开流
     * @param audioInfo 歌曲信息
     * @return 播放时的输出信息
     */
    public OutputInfo read(AudioInfo audioInfo) {
        //刷新设备
        refreshDevice();

        int channels = audioInfo.channels, sampleRate = audioInfo.sampleRate, sampleFormat = audioInfo.sampleFormat;
        boolean resample = false;
        if (audioInfo.channels > cMaxOutputChannels || audioInfo.sampleRate > cMaxOutputSampleRate) {
            channels = Math.min(audioInfo.channels, cMaxOutputChannels);
            sampleRate = Math.min(audioInfo.sampleRate, cMaxOutputSampleRate);
            resample = true;
        }

        if (audioInfo.isPlanar) {
            logger.info("格式 {} 为非平面格式，转化为平面格式",
                    AudioUtil.getSampleFormatName(sampleFormat));
            resample = true;
        }

        if (!AudioUtil.isSupport(sampleFormat)) {
            logger.info("格式 {} 不支持，转化为 {}",
                    AudioUtil.getSampleFormatName(sampleFormat),
                    AudioUtil.getSampleFormatName(AV_SAMPLE_FMT_FLT)
                    );
            sampleFormat = AV_SAMPLE_FMT_FLT;
            resample = true;
        }

        if (Config.api().equals("WASAPI")) {
            int defaultSampleRate = (int) getDeviceInfo(cId).defaultSampleRate;
            if (sampleRate != defaultSampleRate) {
                logger.info("使用WASAPI，歌曲采样率必须为 {} HZ", defaultSampleRate);
                sampleRate = defaultSampleRate;
                resample = true;
            }
        }

        openStream(channels, AudioUtil.getPortAudioSampleFormat(sampleFormat), sampleRate);

        return new OutputInfo(resample, channels, sampleRate, AudioUtil.getSampleFormatNoPlanar(sampleFormat));
    }

    /**
     * 打开统一的流，输出信息为将所有歌曲重采样为特定格式<br>
     * channels: 2<br>
     * sampleRate: 48000Hz<br>
     * sampleFormat: <br>
     * AV_SAMPLE_FMT_FLT(FFmpeg);<br>
     * FORMAT_FLOAT_32(PortAudio)
     * @return 播放时的输出信息
     */
    public OutputInfo readForSameOut() {
        //刷新设备
        refreshDevice();

        int channels = 2;
        int sampleRate = 48000;
        if (Config.api().equals("WASAPI")) {
            int onlySampleRate = (int) getDeviceInfo(cId).defaultSampleRate;
            logger.info("使用WASAPI，所有歌曲的采样率统一为: {}", onlySampleRate);
            sampleRate = onlySampleRate;
        }
        boolean resample = true;

        openStream(channels, FORMAT_FLOAT_32, sampleRate);

        return new OutputInfo(resample, channels, sampleRate, AV_SAMPLE_FMT_FLT);
    }

    /**
     * 为音频设备打开定制的流
     * @param channels 声道数
     * @param sampleFormat 采样格式
     * @param sampleRate 采样数
     */
    private void openStream(int channels, int sampleFormat, int sampleRate) {
        DeviceInfo deviceInfo = getDeviceInfo(cId);
        StreamParameters parameters = new StreamParameters();
        parameters.device = cId;
        parameters.channelCount = channels;
        parameters.sampleFormat = sampleFormat;
        parameters.suggestedLatency = deviceInfo.defaultLowOutputLatency;

        stream = PortAudio.openStream(
                null,
                parameters,
                sampleRate,
                0,
                0
        );
        StreamInfo info = stream.getInfo();
        logger.info("打开流。OutputLatency: {}, SampleRate: {}",
                info.outputLatency, info.sampleRate);
    }

    /**
     * 在当前线程阻塞播放
     */
    @Override
    public void run() {
        if (stream == null) return;
        if (!stream.isActive())
            stream.start();

        int ret;
        if ((ret = stream.getWriteAvailable()) < 0) {
            logger.fatal("当前流无法写入: {}", ret);
            ret = Global.message(
                    Win32.getLongHWND(FPAScreen.mainWindow),
                    "出错",
                    "歌曲的流无法写入设备: " + ret + "，可能是设备问题或请重试",
                    MessageFlags.DisplayButtons.RETRY_CANCEL | MessageFlags.Icons.WARNING
            );
            if (ret == MessageFlags.ReturnValue.RETRY) run();
            CurrentStatus.to(CurrentStatus.Status.STOP);
            buffer.clear();
        }
        while (!buffer.isEmpty() || !CurrentStatus.is(CurrentStatus.Status.STOP)) {//当解码完成同时缓冲区内没有数据时才停止
            boolean paused;//用于判断是否暂停了的标识，如果是从暂停中启动，则为true。防止提前退出
            try {
                paused = CurrentStatus.waitUntilNotPaused();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (CurrentStatus.is(CurrentStatus.Status.STOP) && paused) {
                buffer.clear();
                break;
            }

            Buffer.Data<?> data = buffer.take();
            if (data.type == null) {
                //解码器解码完成
                if (finishPerSong != null)
                    finishPerSong.onFinish((Integer) data.data);
            } else {
                //正常播放解码信息
                start.handler(data.old_samples);
                boolean result = false;
                switch (data.type) {
                    case SHORT -> {
                        result = stream.write((short[]) data.data, data.nb_samples);
                        ArrayLoop.returnArray((short[]) data.data);
                    }
                    case FLOAT -> {
                        result = stream.write((float[]) data.data, data.nb_samples);
                        ArrayLoop.returnArray((float[]) data.data);
                    }
                }
                if (result) {
                    logger.fatal("Write stream failed: output underflow");
                }
            }
        }
        stop();
        event.onFinish(NO_ARGS);
    }

    /**
     * 在新线程中播放
     */
    @Override
    public void start() {
        //调用在线程池中睡眠的线程，启动播放
        singleThread.submit(this);
    }

    public void stop() {
        //先将流中数据完后暂停，然后停止
        stream.stop();

        stream.close();
    }

    public void terminate() {
        singleThread.submit(() -> {
            if (!stream.isStopped()) stop();

            PortAudio.terminate();
        });

        singleThread.shutdown();
        try {
            if (!singleThread.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                singleThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            singleThread.shutdownNow();
        }
    }

    @FunctionalInterface
    public interface PlaySample {
        void handler(int samples);
    }
}
