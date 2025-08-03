package org.balinhui.fpa.info;

import java.util.Map;

import static org.balinhui.portaudio.PaSampleFormat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class AudioInfo {
    public int channels;
    public int sampleFormat;
    public int sampleRate;
    public byte[] cover;
    public Map<String, String> metaData;
    public float durationSeconds;

    public AudioInfo(int channels, int sampleFormat, int sampleRate, byte[] cover, Map<String, String> metaData, float durationSeconds) {
        this.channels = channels;
        this.sampleFormat = sampleFormat;
        this.sampleRate = sampleRate;
        this.cover = cover;
        this.metaData = metaData;
        this.durationSeconds = durationSeconds;
    }

    public long getPortAudioSampleFormat() {
        return switch (sampleFormat){
            case AV_SAMPLE_FMT_U8, AV_SAMPLE_FMT_U8P -> paUInt8;
            case AV_SAMPLE_FMT_S16, AV_SAMPLE_FMT_S16P -> paInt16;
            case AV_SAMPLE_FMT_S32, AV_SAMPLE_FMT_S32P -> paInt32;
            case AV_SAMPLE_FMT_FLT, AV_SAMPLE_FMT_FLTP -> paFloat32;
            default -> -1;
        };
    }
}
