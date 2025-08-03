package org.balinhui.fpa.core;

import com.sun.jna.ptr.PointerByReference;
import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.portaudio.*;

import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16;

public class Player implements Runnable {
    private static final PortAudio pa = PortAudio.INSTANCE;
    private final Buffer buffer = new Buffer();
    private static final Player player = new Player();
    private final int maxOutputChannels;
    private final int maxOutputSampleRate;
    private PaStream stream;
    private PlayFinished finished;
    private PlaySample start;
    private boolean playedContinue = false;

    public static Player getPlayer() {
        return player;
    }

    public void setFinished(PlayFinished finished) {
        this.finished = finished;
    }

    public void setBeforePlaySample(PlaySample start) {
        this.start = start;
    }

    private Player() {
        int err = pa.Pa_Initialize();
        if (err != PortAudio.paNoError) {
            throw new RuntimeException("Player Init Failed: " + pa.Pa_GetErrorText(err));
        }
        int id = pa.Pa_GetDefaultOutputDevice();
        PaDeviceInfo deviceInfo = pa.Pa_GetDeviceInfo(id);
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
    }

    public OutputInfo read(AudioInfo audioInfo) {
        playedContinue = false;
        int channels = audioInfo.channels, sampleRate = audioInfo.sampleRate;
        boolean resample = false;
        if (audioInfo.channels > maxOutputChannels || audioInfo.sampleRate > maxOutputSampleRate) {
            channels = Math.min(audioInfo.channels, maxOutputChannels);
            sampleRate = Math.min(audioInfo.sampleRate, maxOutputSampleRate);
            resample = true;
        }
        PointerByReference streamRef = new PointerByReference();
        int id = pa.Pa_GetDefaultOutputDevice();
        PaDeviceInfo deviceInfo = pa.Pa_GetDeviceInfo(id);
        PaStreamParameters parameters = new PaStreamParameters();
        parameters.device = id;
        parameters.channelCount = channels;
        parameters.sampleFormat = audioInfo.getPortAudioSampleFormat();
        parameters.suggestedLatency = deviceInfo.defaultLowOutputLatency;
        int err = pa.Pa_OpenStream(
                streamRef,
                null,
                parameters,
                sampleRate,
                256,
                PaStreamFlags.paNoFlag,
                null,
                null
        );
        if (err != PortAudio.paNoError) throw new RuntimeException("Open stream failed: " + pa.Pa_GetErrorText(err));
        stream = new PaStream(streamRef);
        return new OutputInfo(resample, channels, sampleRate, audioInfo.sampleFormat);
    }

    public OutputInfo read1(AudioInfo audioInfo) {
        playedContinue = true;
        int channels = 2, sampleRate = 44100, sampleFormat = AV_SAMPLE_FMT_S16;
        boolean resample = audioInfo.channels != channels || audioInfo.sampleRate != sampleRate || audioInfo.sampleFormat != sampleFormat;
        PointerByReference streamRef = new PointerByReference();
        int id = pa.Pa_GetDefaultOutputDevice();
        PaDeviceInfo deviceInfo = pa.Pa_GetDeviceInfo(id);
        PaStreamParameters parameters = new PaStreamParameters();
        parameters.device = id;
        parameters.sampleFormat = PaSampleFormat.paInt16;
        parameters.channelCount = channels;
        parameters.suggestedLatency = deviceInfo.defaultLowOutputLatency;
        int err = pa.Pa_OpenStream(
                streamRef,
                null,
                parameters,
                sampleRate,
                256,
                PaStreamFlags.paNoFlag,
                null,
                null
        );
        if (err != PortAudio.paNoError) throw new RuntimeException("Open stream failed: " + pa.Pa_GetErrorText(err));
        stream = new PaStream(streamRef);
        return new OutputInfo(resample, channels, sampleRate, sampleFormat);
    }

    @Override
    public void run() {
        pa.Pa_StartStream(stream);
        int err = 0;
        while (CurrentStatus.currentStatus == CurrentStatus.Status.PLAYING || !buffer.isEmpty()) {
            Buffer.Data<?> data = buffer.take();
            if (start != null)
                start.handler(data.getNb_samples());
            switch (data.getType()) {
                case BYTE -> {
                    err = pa.Pa_WriteStream(stream, (byte[]) data.getData(), data.getNb_samples());
                    ArrayLoop.returnArray((byte[]) data.getData());
                }
                case SHORT -> {
                    err = pa.Pa_WriteStream(stream, (short[]) data.getData(), data.getNb_samples());
                    ArrayLoop.returnArray((short[]) data.getData());
                }
                case INTEGER -> {
                    err = pa.Pa_WriteStream(stream, (int[]) data.getData(), data.getNb_samples());
                    ArrayLoop.returnArray((int[]) data.getData());
                }
                case FLOAT -> {
                    err = pa.Pa_WriteStream(stream, (float[]) data.getData(), data.getNb_samples());
                    ArrayLoop.returnArray((float[]) data.getData());
                }
            }
            if (err != PortAudio.paNoError) throw new RuntimeException("Write stream failed: " + pa.Pa_GetErrorText(err));
        }
        if (!playedContinue && finished != null)
            finished.handler();
    }



    public void stop() {
        int err;
        err = pa.Pa_StopStream(stream);
        if (err != PortAudio.paNoError) throw new RuntimeException("Stop stream failed: " + pa.Pa_GetErrorText(err));
        err = pa.Pa_CloseStream(stream);
        if (err != PortAudio.paNoError) throw new RuntimeException("Close stream failed: " + pa.Pa_GetErrorText(err));
    }

    public void terminate() {
        int err;
        err = pa.Pa_Terminate();
        if (err != PortAudio.paNoError) throw new RuntimeException("Terminate failed: " + pa.Pa_GetErrorText(err));
    }

    public int getMaxOutputChannels() {
        return maxOutputChannels;
    }

    public int getMaxOutputSampleRate() {
        return maxOutputSampleRate;
    }

    @FunctionalInterface
    public interface PlaySample {
        void handler(int nb_samples);
    }

    @FunctionalInterface
    public interface PlayFinished {
        void handler();
    }
}
