package org.balinhui.fpa.util;

import org.bytedeco.javacpp.BytePointer;

import static org.balinhui.portaudio.PaSampleFormat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.avutil.av_get_sample_fmt_name;

public class AudioUtil {
    private AudioUtil() {}

    public static int getSampleFormatNoPlanar(int sampleFormat) {
        return switch (sampleFormat) {
            case AV_SAMPLE_FMT_U8P -> AV_SAMPLE_FMT_U8;
            case AV_SAMPLE_FMT_S16P -> AV_SAMPLE_FMT_S16;
            case AV_SAMPLE_FMT_S32P -> AV_SAMPLE_FMT_S32;
            case AV_SAMPLE_FMT_FLTP -> AV_SAMPLE_FMT_FLT;
            default -> sampleFormat;
        };
    }

    public static long getPortAudioSampleFormat(int sampleFormat) {
        return switch (sampleFormat) {
            case AV_SAMPLE_FMT_U8, AV_SAMPLE_FMT_U8P -> paUInt8;
            case AV_SAMPLE_FMT_S16, AV_SAMPLE_FMT_S16P -> paInt16;
            case AV_SAMPLE_FMT_S32, AV_SAMPLE_FMT_S32P -> paInt32;
            case AV_SAMPLE_FMT_FLT, AV_SAMPLE_FMT_FLTP -> paFloat32;
            default -> -1;
        };
    }

    public static boolean isSupport(int sampleFormat) {
        return switch (sampleFormat) {
            case AV_SAMPLE_FMT_S16, AV_SAMPLE_FMT_S16P, AV_SAMPLE_FMT_FLT, AV_SAMPLE_FMT_FLTP -> true;
            default -> false;
        };
    }

    public static String getSampleFormatName(int sampleFormat) {
        try(BytePointer namePointer = av_get_sample_fmt_name(sampleFormat)) {
            return namePointer.getString();
        }
    }
}
