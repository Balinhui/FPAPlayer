package org.balinhui.fpa.core;

import com.sun.jna.ptr.PointerByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;
import org.balinhui.fpa.portaudioforjava.*;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.fpa.util.AudioUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLT;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16;

public class Player implements Runnable, AudioHandler {
    private static final Logger logger = LogManager.getLogger(Player.class);
    private static final PortAudio pa = PortAudio.INSTANCE;
    private final Buffer buffer = new Buffer();
    private static final Player player = new Player();
    private int cId;//Current device id
    private int cMaxOutputChannels;//current
    private int cMaxOutputSampleRate;//current
    private PaStream stream;
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
            PortAudio.setUTF_8();//设置Port Audio输出字符格式为UTF-8
            int err = pa.Pa_Initialize();
            if (err != PortAudio.paNoError) {
                logger.fatal("Player Init Failed: {}", pa.Pa_GetErrorText(err));
                throw new RuntimeException("Player Init Failed: " + pa.Pa_GetErrorText(err));
            }

            //公布PortAudio版本
            logger.info("当前PortAudio版本: {}", pa.Pa_GetVersionText());

            //获取设备信息
            int id = pa.Pa_GetDefaultOutputDevice();
            this.cId = id;
            PaDeviceInfo deviceInfo = pa.Pa_GetDeviceInfo(id);
            logger.info("默认输出设备为: {}", deviceInfo.name);

            //取得当前的设备的最大输出声道数和采样率
            getDeviceChannelsAndSampleRateInfo(id, deviceInfo);

            logger.info("设备最大支持采样率: {} Hz", cMaxOutputSampleRate);
        });
    }

    private void getDeviceChannelsAndSampleRateInfo(int id, PaDeviceInfo deviceInfo) {
        this.cMaxOutputChannels = deviceInfo.maxOutputChannels;
        PaStreamParameters test = new PaStreamParameters();
        test.device = id;
        test.channelCount = this.cMaxOutputChannels;
        test.sampleFormat = PaSampleFormat.paFloat32;
        test.suggestedLatency = deviceInfo.defaultLowOutputLatency;
        double[] sampleRatesToTest = {384000.0, 192000.0, 96000.0, 88200.0, 48000.0, 44100.0, 8000.0};
        int maxOutputSampleRate = (int) sampleRatesToTest[sampleRatesToTest.length - 1];
        for (double v : sampleRatesToTest) {
            if (pa.Pa_IsFormatSupported(null, test, v) == PortAudio.paFormatIsSupported) {
                maxOutputSampleRate = (int) v;
                break;
            }
        }
        this.cMaxOutputSampleRate = maxOutputSampleRate;
    }

    /**
     * 分析当前歌曲，决定是否需要重采样，同时打开流
     * @param audioInfo 歌曲信息
     * @return 播放时的输出信息
     */
    public OutputInfo read(AudioInfo audioInfo) {
        //刷新设备
        int id = pa.Pa_GetDefaultOutputDevice();
        if (id != cId) {
            logger.info("检测到输出设备更改，由id: {} -> id: {}", cId, id);
            this.cId = id;
            PaDeviceInfo deviceInfo = pa.Pa_GetDeviceInfo(id);
            logger.trace("取得当前输出设备: {}", deviceInfo.name);

            getDeviceChannelsAndSampleRateInfo(id, deviceInfo);
        }


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

        openStream(channels, AudioUtil.getPortAudioSampleFormat(sampleFormat), sampleRate);

        return new OutputInfo(resample, channels, sampleRate, AudioUtil.getSampleFormatNoPlanar(sampleFormat));
    }

    /**
     * 打开统一的流，输出信息为将所有歌曲重采样为特定格式<br>
     * channels: 2<br>
     * sampleRate: 44100Hz<br>
     * sampleFormat: <br>
     * AV_SAMPLE_FMT_S16(FFmpeg);<br>
     * paInt16(PortAudio)
     * @return 播放时的输出信息
     */
    public OutputInfo readForSameOut() {
        int channels = 2;
        int sampleRate = 44100;
        boolean resample = true;

        openStream(channels, PaSampleFormat.paInt16, sampleRate);

        return new OutputInfo(resample, channels, sampleRate, AV_SAMPLE_FMT_S16);
    }

    /**
     * 为音频设备打开定制的流
     * @param channels 声道数
     * @param sampleFormat 采样格式
     * @param sampleRate 采样数
     */
    private void openStream(int channels, long sampleFormat, double sampleRate) {
        PointerByReference streamRef = new PointerByReference();
        PaDeviceInfo deviceInfo = pa.Pa_GetDeviceInfo(cId);
        PaStreamParameters parameters = new PaStreamParameters();
        parameters.device = cId;
        parameters.channelCount = channels;
        parameters.sampleFormat = sampleFormat;
        parameters.suggestedLatency = deviceInfo.defaultLowOutputLatency;
        int err = pa.Pa_OpenStream(
                streamRef,
                null,
                parameters,
                sampleRate,
                PortAudio.paFramesPerBufferUnspecified,
                PaStreamFlags.paNoFlag,
                null,
                null
        );
        if (err != PortAudio.paNoError) {
            logger.fatal("Open stream failed: {}", pa.Pa_GetErrorText(err));
            throw new RuntimeException("Open stream failed: " + pa.Pa_GetErrorText(err));
        }
        stream = new PaStream(streamRef);
        logger.info("打开流");
    }

    /**
     * 在当前线程阻塞播放
     */
    @Override
    public void run() {
        int err = pa.Pa_StartStream(stream);
        if (err != PortAudio.paNoError) {
            logger.fatal("开始流失败");
            throw new RuntimeException("开始流失败");
        }
        if (pa.Pa_GetStreamWriteAvailable(stream) != 0) {
            logger.fatal("当前流无法写入");
            throw new RuntimeException("当前流无法写入");
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
                switch (data.type) {
                    case SHORT -> {
                        err = pa.Pa_WriteStream(stream, (short[]) data.data, data.nb_samples);
                        ArrayLoop.returnArray((short[]) data.data);
                    }
                    case FLOAT -> {
                        err = pa.Pa_WriteStream(stream, (float[]) data.data, data.nb_samples);
                        ArrayLoop.returnArray((float[]) data.data);
                    }
                }
                if (err != PortAudio.paNoError) {
                    logger.fatal("Write stream failed: {}", pa.Pa_GetErrorText(err));
                    if (err != -9980)//放过Output underflowed一马
                        throw new RuntimeException("Write stream failed: " + pa.Pa_GetErrorText(err));
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
        int err;
        err = pa.Pa_StopStream(stream);
        if (err != PortAudio.paNoError) {
            logger.fatal("Stop stream failed: {}", pa.Pa_GetErrorText(err));
            throw new RuntimeException("Stop stream failed: " + pa.Pa_GetErrorText(err));
        }
        err = pa.Pa_CloseStream(stream);
        if (err != PortAudio.paNoError) {
            logger.fatal("Close stream failed: {}", pa.Pa_GetErrorText(err));
            throw new RuntimeException("Close stream failed: " + pa.Pa_GetErrorText(err));
        }
    }

    public void terminate() {
        singleThread.submit(() -> {
            if (!pa.Pa_IsStreamStopped(stream)) {
                System.out.println("exe");
                stop();
            }
            int err;
            err = pa.Pa_Terminate();
            if (err != PortAudio.paNoError) {
                logger.fatal("Terminate failed: {}", pa.Pa_GetErrorText(err));
                throw new RuntimeException("Terminate failed: " + pa.Pa_GetErrorText(err));
            }
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
