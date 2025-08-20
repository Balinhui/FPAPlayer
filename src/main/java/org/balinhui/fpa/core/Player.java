package org.balinhui.fpa.core;

import com.sun.jna.ptr.PointerByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.fpa.util.AudioUtil;
import org.balinhui.portaudio.*;

import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLT;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16;

public class Player implements Runnable, AudioHandler {
    private static final Logger logger = LogManager.getLogger(Player.class);
    private static final PortAudio pa = PortAudio.INSTANCE;
    private final Buffer buffer = new Buffer();
    private static final Player player = new Player();
    private final int maxOutputChannels;
    private final int maxOutputSampleRate;
    private PaStream stream;
    private FinishEvent event;//当播放循环结束后会调用
    private PlaySample start;//播放循环期间每一帧调用一次
    private Thread play;

    public static Player getPlayer() {
        return player;
    }

    @Override
    public void setOnFinished(FinishEvent event) {
        this.event = event;
    }

    public void setBeforePlaySample(PlaySample start) {
        this.start = start;
    }

    private Player() {
        PortAudio.setUTF_8();//设置Port Audio输出字符格式为UTF-8
        int err = pa.Pa_Initialize();
        if (err != PortAudio.paNoError) {
            logger.fatal("Player Init Failed: {}", pa.Pa_GetErrorText(err));
            throw new RuntimeException("Player Init Failed: " + pa.Pa_GetErrorText(err));
        }
        int id = pa.Pa_GetDefaultOutputDevice();
        PaDeviceInfo deviceInfo = pa.Pa_GetDeviceInfo(id);
        logger.info("默认输出设备为: {}", deviceInfo.name);
        maxOutputChannels = deviceInfo.maxOutputChannels;
        PaStreamParameters test = new PaStreamParameters();
        test.device = id;
        test.channelCount = maxOutputChannels;
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
        this.maxOutputSampleRate = maxOutputSampleRate;
        logger.info("设备最大支持采样率: {}", maxOutputSampleRate);
    }

    /**
     * 分析当前歌曲，决定是否需要重采样，同时打开流
     * @param audioInfo 歌曲信息
     * @return 播放时的输出信息
     */
    public OutputInfo read(AudioInfo audioInfo) {
        int channels = audioInfo.channels, sampleRate = audioInfo.sampleRate, sampleFormat = audioInfo.sampleFormat;
        boolean resample = false;
        if (audioInfo.channels > maxOutputChannels || audioInfo.sampleRate > maxOutputSampleRate) {
            channels = Math.min(audioInfo.channels, maxOutputChannels);
            sampleRate = Math.min(audioInfo.sampleRate, maxOutputSampleRate);
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
     * sampleFormat: AV_SAMPLE_FMT_S16(FFmpeg), paInt16(PortAudio)
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
        int id = pa.Pa_GetDefaultOutputDevice();
        PaDeviceInfo deviceInfo = pa.Pa_GetDeviceInfo(id);
        PaStreamParameters parameters = new PaStreamParameters();
        parameters.device = id;
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
            if (start != null)
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
        stop();
        if (event != null)
            event.onFinish(NO_ARGS);
    }

    /**
     * 在新线程中播放
     */
    @Override
    public void start() {
        if (play == null || play.getState() == Thread.State.TERMINATED) {
            play = new Thread(this);
            play.setName("Play Thread");
        }

        if (play.getState() == Thread.State.NEW) {
            play.start();
            logger.info("播放线程启动");
        }
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
        int err;
        err = pa.Pa_Terminate();
        if (err != PortAudio.paNoError) {
            logger.fatal("Terminate failed: {}", pa.Pa_GetErrorText(err));
            throw new RuntimeException("Terminate failed: " + pa.Pa_GetErrorText(err));
        }
    }

    public int getMaxOutputChannels() {
        return maxOutputChannels;
    }

    public int getMaxOutputSampleRate() {
        return maxOutputSampleRate;
    }

    @FunctionalInterface
    public interface PlaySample {
        void handler(int samples);
    }
}
