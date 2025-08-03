package org.balinhui.fpa.core;

import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.fpa.util.Resample;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.*;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Decoder implements Runnable {
    private String path;
    private final Buffer buffer = new Buffer();
    private static final Decoder decoder = new Decoder();
    private OutputInfo outputInfo;

    public static Decoder getDecoder() {
        return decoder;
    }

    private Decoder() {
    }

    public AudioInfo read(@NotNull String path) {
        this.path = path;
        AVFormatContext fmtCtx = new AVFormatContext(null);
        int coverStream = -1;
        AVCodecParameters codecPar = null;
        AVPacket coverPkt;
        try {
            if (avformat_open_input(fmtCtx, path, null, null) < 0) {
                System.err.println("Open file failed");
                return new AudioInfo(0, 0, 0, null, null, 0F);
            }
            if (avformat_find_stream_info(fmtCtx, (PointerPointer<?>) null) < 0) {
                System.err.println("Cant find stream info");
                return new AudioInfo(0, 0, 0, null, null, 0F);
            }
            AVStream stream = null;
            for (int i = 0; i < fmtCtx.nb_streams(); i++) {
                if (fmtCtx.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO &&
                    (fmtCtx.streams(i).disposition() & AV_DISPOSITION_ATTACHED_PIC) != 0) {
                    coverStream = i;
                } else if (fmtCtx.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
                    stream = fmtCtx.streams(i);
                    codecPar = stream.codecpar();
                }
            }
            if (codecPar == null) {
                System.err.println("Doesnt find codec parameter");
                return new AudioInfo(0, 0, 0, null, null, 0F);
            }
            if (coverStream == -1) {
                System.err.println("Doesnt find cover");
            }
            coverPkt = fmtCtx.streams(coverStream).attached_pic();
            byte[] coverData = null;
            if (coverPkt != null && coverPkt.data() != null && coverPkt.size() > 0) {
                coverData = new byte[coverPkt.size()];
                coverPkt.data().get(coverData);
            }
            return new AudioInfo(
                    codecPar.ch_layout().nb_channels(),
                    codecPar.format(),
                    codecPar.sample_rate(),
                    coverData,
                    getMetaData(fmtCtx, stream),
                    fmtCtx.duration() / (float) AV_TIME_BASE
            );
        } finally {
            avformat_close_input(fmtCtx);
            avformat_free_context(fmtCtx);
        }
    }

    private Map<String, String> getMetaData(AVFormatContext fmtCtx, AVStream stream) {
        Map<String, String> metadata = new HashMap<>();
        AVDictionary dictionary = stream.metadata() == null ? fmtCtx.metadata() : stream.metadata();
        AVDictionaryEntry entry = null;
        while ((entry = av_dict_get(dictionary, "", entry, AV_DICT_IGNORE_SUFFIX)) != null) {
            metadata.put(
                    entry.key().getString(Charset.defaultCharset()),
                    entry.value().getString(Charset.defaultCharset())
            );
        }
        String[] l = {"lyrics", "LYRICS", "lyrics-XXX"};
        for (String s : l) {
            if (metadata.containsKey(s)) return metadata;
        }
        if ((entry = av_dict_get(fmtCtx.metadata(), "lyrics", null, AV_DICT_IGNORE_SUFFIX)) != null) {
            metadata.put(
                    entry.key().getString(Charset.defaultCharset()),
                    entry.value().getString(Charset.defaultCharset())
            );
        }
        return metadata;
    }

    public void setOutput(OutputInfo info) {
        this.outputInfo = info;
    }

    @Override
    public void run() {
        if (path == null) return;
        CurrentStatus.currentStatus = CurrentStatus.Status.PLAYING;
        AVFormatContext fmtCtx = new AVFormatContext(null);
        if (avformat_open_input(fmtCtx, path, null, null) < 0) {
            throw new RuntimeException("Open file failed");
        }
        if (avformat_find_stream_info(fmtCtx, (PointerPointer<?>) null) < 0) {
            throw new RuntimeException("Cant find stream info");
        }
        int streamIndex = -1;
        AVCodecParameters codecPar = null;
        AVCodec codec = null;
        for (int i = 0; i < fmtCtx.nb_streams(); i++) {
            if (fmtCtx.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
                streamIndex = i;
                codecPar = fmtCtx.streams(i).codecpar();
                codec = avcodec_find_decoder(codecPar.codec_id());
            }
        }
        if (streamIndex == -1 || codec == null) {
            throw new RuntimeException("Doesnt find audio stream or decoder");
        }
        AVCodecContext codecCtx = avcodec_alloc_context3(codec);
        if (codecCtx.isNull()) throw new RuntimeException("Doest allocate context");
        if (avcodec_parameters_to_context(codecCtx, codecPar) < 0) {
            throw new RuntimeException("Cant copy parameters to context");
        }
        if (avcodec_open2(codecCtx, codec, (PointerPointer<?>) null) < 0) {
            throw new RuntimeException("Cant open decoder");
        }

        int srcChannels = codecCtx.ch_layout().nb_channels(), dstChannels;
        int srcSampleFormat = codecCtx.sample_fmt(), dstSampleFormat;
        int pointerSize;
        Resample resample = null;
        boolean needsResample = outputInfo != null && outputInfo.resample;
        if (needsResample) {
            dstChannels = outputInfo.channels;
            dstSampleFormat = outputInfo.sampleFormat;
            resample = new Resample(
                    srcChannels,
                    codecCtx.sample_rate(),
                    srcSampleFormat,
                    outputInfo
            );
            pointerSize = resample.getPointerSize();
        } else {
            dstChannels = srcChannels;
            dstSampleFormat = srcSampleFormat;
            pointerSize = av_sample_fmt_is_planar(dstSampleFormat) == 1 ? dstChannels : 1;
        }

        AVPacket packet = av_packet_alloc();
        AVFrame frame = av_frame_alloc();
        if (packet.isNull() || frame.isNull()) {
            throw new RuntimeException("Cant allocate packet or frame");
        }
        BytePointer[] rawData = new BytePointer[pointerSize];
        mainloop : while (av_read_frame(fmtCtx, packet) >= 0 && CurrentStatus.currentStatus == CurrentStatus.Status.PLAYING) {
            if (packet.stream_index() == streamIndex) {
                int ret = avcodec_send_packet(codecCtx, packet);
                if (ret < 0) break;
                while (avcodec_receive_frame(codecCtx, frame) >= 0) {
                    int samples = frame.nb_samples();
                    if (needsResample) {
                        samples = resample.process(rawData, samples, frame.data());
                    } else {
                        for (int i = 0; i < pointerSize; i++) {
                            rawData[i] = frame.data(i);
                        }
                    }
                    int arraySize = samples * dstChannels;

                    switch (dstSampleFormat) {
                        case AV_SAMPLE_FMT_U8 -> {
                            byte[] byteData = ArrayLoop.getArray(arraySize, byte[].class);
                            byteData = ArrayLoop.reSize(byteData, arraySize);
                            rawData[0].get(byteData);
                            buffer.put(Buffer.Data.of(samples, byteData));
                        }
                        case AV_SAMPLE_FMT_S16 -> {
                            short[] shortData = ArrayLoop.getArray(arraySize, short[].class);
                            shortData = ArrayLoop.reSize(shortData, arraySize);
                            ShortPointer data = new ShortPointer(rawData[0]);
                            data.get(shortData);
                            buffer.put(Buffer.Data.of(samples, shortData));
                        }
                        case AV_SAMPLE_FMT_S32 -> {
                            int[] intData = ArrayLoop.getArray(arraySize, int[].class);
                            intData = ArrayLoop.reSize(intData, arraySize);
                            IntPointer data = new IntPointer(rawData[0]);
                            data.get(intData);
                            buffer.put(Buffer.Data.of(samples, intData));
                        }
                        case AV_SAMPLE_FMT_FLT -> {
                            float[] floatData = ArrayLoop.getArray(arraySize, float[].class);
                            floatData = ArrayLoop.reSize(floatData, arraySize);
                            FloatPointer data = new FloatPointer(rawData[0]);
                            data.get(floatData);
                            buffer.put(Buffer.Data.of(samples, floatData));
                        }
                        case AV_SAMPLE_FMT_FLTP -> {
                            float[] floatData = ArrayLoop.getArray(arraySize, float[].class);
                            floatData = ArrayLoop.reSize(floatData, arraySize);
                            FloatPointer[] data = new FloatPointer[dstChannels];
                            for (int i = 0; i < data.length; i++) {
                                data[i] = new FloatPointer(rawData[i]);
                            }
                            for (int i = 0; i < samples; i++) {
                                for (int ch = 0; ch < dstChannels; ch++)
                                    floatData[i * dstChannels + ch] = data[ch].get(i);
                            }
                            buffer.put(Buffer.Data.of(samples, floatData));
                        }
                        default -> {
                            System.out.print(av_get_sample_fmt_name(dstSampleFormat).getString());
                            System.out.println("  not support......");
                            break mainloop;
                        }
                    }
                    av_frame_unref(frame);
                }
            }
            av_packet_unref(packet);
        }
        if (resample != null)
            resample.free();
        avformat_close_input(fmtCtx);
        avformat_free_context(fmtCtx);
        avcodec_free_context(codecCtx);
        av_frame_free(frame);
        av_packet_free(packet);
        CurrentStatus.currentStatus = CurrentStatus.Status.STOP;
    }
}
