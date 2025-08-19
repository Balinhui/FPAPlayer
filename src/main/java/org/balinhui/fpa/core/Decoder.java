package org.balinhui.fpa.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;
import org.balinhui.fpa.util.ArrayLoop;
import org.balinhui.fpa.util.FlacCoverExtractor;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Decoder implements Runnable, AudioHandler {
    private static final Logger logger = LogManager.getLogger(Decoder.class);
    private String[] paths;
    private final Buffer buffer = new Buffer();
    private static final Decoder decoder = new Decoder();
    private OutputInfo outputInfo;
    private int currentProgress = 0;//当路径中有多首歌时，用于指示播放进度
    private FinishEvent event;//解码完一首歌后调用
    private Thread decode;//解码线程

    private double currentTime = 0.0;//单首歌播放时长 (秒)

    public static Decoder getDecoder() {
        return decoder;
    }

    private Decoder() {
        av_log_set_level(AV_LOG_FATAL);
    }

    /**
     * 读取单个文件，准备播放
     * @param path 文件路径
     * @return 读取的音频信息
     */
    public AudioInfo read(@NotNull String path) {
        return read(new String[]{path});
    }

    /**
     * 读取多个文件，准备播放
     * @param paths 文件路径数组
     * @return 读取的第一个音频信息
     */
    public AudioInfo read(@NotNull String[] paths) {
        this.paths = paths;
        return readOnly(paths[currentProgress]);
    }

    /**
     * 读取单个文件，不准备播放
     * @param path 文件路径
     * @return 读取的音频信息
     */
    public AudioInfo readOnly(@NotNull String path) {
        logger.trace("解码器读取: {}", path);
        AVFormatContext fmtCtx = new AVFormatContext(null);
        int coverStream = -1;
        AVCodecParameters codecPar = null;
        AVPacket coverPkt;
        try {
            if (avformat_open_input(fmtCtx, path, null, null) < 0) {
                logger.error("Open file failed");
                return new AudioInfo(0, 0, 0, null, null, 0F);
            }
            logger.trace("打开文件");
            if (avformat_find_stream_info(fmtCtx, (PointerPointer<?>) null) < 0) {
                logger.error("Cant find stream info");
                return new AudioInfo(0, 0, 0, null, null, 0F);
            }
            logger.trace("寻找流信息");
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
                logger.error("Doesnt find codec parameter");
                return new AudioInfo(0, 0, 0, null, null, 0F);
            }
            logger.trace("找到解码器参数");
            byte[] coverData = null;
            if (coverStream == -1) {
                try {
                    logger.info("ffmpeg无法找到封面，尝试通过文件提取");
                    coverData = FlacCoverExtractor.extractFlacCover(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (coverData == null)
                    logger.error("Doesnt find cover");
            } else {
                coverPkt = fmtCtx.streams(coverStream).attached_pic();
                if (coverPkt != null && coverPkt.data() != null && coverPkt.size() > 0) {
                    coverData = new byte[coverPkt.size()];
                    coverPkt.data().get(coverData);
                }
                logger.trace("获取封面");
            }
            return new AudioInfo(
                    codecPar.ch_layout().nb_channels(),
                    codecPar.format(),
                    codecPar.sample_rate(),
                    coverData,
                    getMetadata(fmtCtx, stream),
                    fmtCtx.duration() / (float) AV_TIME_BASE
            );
        } finally {
            avformat_close_input(fmtCtx);
            avformat_free_context(fmtCtx);
            fmtCtx.deallocate();
            if (codecPar != null) codecPar.deallocate();
            logger.trace("释放资源");
        }
    }

    /**
     * 读取歌曲元数据
     * @param fmtCtx ffmpeg读取好的音频内容
     * @param stream ffmpeg读取好的音频流
     * @return 包含元数据的键值对
     */
    private Map<String, String> getMetadata(AVFormatContext fmtCtx, AVStream stream) {
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
        logger.warn("总元数据中没有找到歌词信息，开始重点查找");
        if ((entry = av_dict_get(fmtCtx.metadata(), "lyrics", null, AV_DICT_IGNORE_SUFFIX)) != null) {
            metadata.put(
                    entry.key().getString(Charset.defaultCharset()),
                    entry.value().getString(Charset.defaultCharset())
            );
            logger.trace("找到歌词信息");
        }
        return metadata;
    }

    public void setOutput(OutputInfo info) {
        logger.trace("设置输出信息");
        this.outputInfo = info;
    }

    @Override
    public void setOnFinished(FinishEvent event) {
        logger.trace("设置解码完成回调{}", event == null ? ": 为null" : "");
        this.event = event;
    }


    /**
     * 当前线程中阻塞解码
     */
    @Override
    public void run() {
        CurrentStatus.to(CurrentStatus.Status.PLAYING);
        logger.info("设置当前状态为: {}", CurrentStatus.is(CurrentStatus.Status.PLAYING) ? "PLAYING" : "STOP");
        for (String path : paths) {
            if (path == null) {
                logger.warn("路径为null");
                return;
            }
            logger.info("解码开始");
            AVFormatContext fmtCtx = new AVFormatContext(null);
            if (avformat_open_input(fmtCtx, path, null, null) < 0) {
                logger.fatal("Open file failed");
                throw new RuntimeException("Open file failed");
            }
            if (avformat_find_stream_info(fmtCtx, (PointerPointer<?>) null) < 0) {
                logger.fatal("Cant find stream info");
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
                logger.fatal("Doesnt find audio stream or decoder");
                throw new RuntimeException("Doesnt find audio stream or decoder");
            }
            AVCodecContext codecCtx = avcodec_alloc_context3(codec);
            if (codecCtx.isNull()) throw new RuntimeException("Doest allocate context");
            if (avcodec_parameters_to_context(codecCtx, codecPar) < 0) {
                logger.fatal("Cant copy parameters to context");
                throw new RuntimeException("Cant copy parameters to context");
            }
            if (avcodec_open2(codecCtx, codec, (PointerPointer<?>) null) < 0) {
                logger.fatal("Cant open decoder");
                throw new RuntimeException("Cant open decoder");
            }

            //重采样所需
            int srcChannels = codecCtx.ch_layout().nb_channels(), dstChannels;
            int srcSampleFormat = codecCtx.sample_fmt(), dstSampleFormat;
            int pointerSize;
            Resample resample = null;
            final boolean needsResample = outputInfo != null && outputInfo.resample;
            if (needsResample) {
                logger.trace("初始化重采样");
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

            //歌曲部分信息
            int sampleRate = codecCtx.sample_rate();
            BytePointer fmtName = av_get_sample_fmt_name(dstSampleFormat);
            logger.trace("歌曲样本格式: {} ", fmtName.getString());

            AVPacket packet = av_packet_alloc();
            AVFrame frame = av_frame_alloc();
            if (packet.isNull() || frame.isNull()) {
                logger.fatal("Cant allocate packet or frame");
                throw new RuntimeException("Cant allocate packet or frame");
            }
            BytePointer[] rawData = new BytePointer[pointerSize];
            long playedSamples = 0;
            currentTime = 0.0;
            mainloop:
            while (CurrentStatus.is(CurrentStatus.Status.PLAYING)) {

                if (av_read_frame(fmtCtx, packet) < 0) break;
                if (packet.stream_index() == streamIndex) {
                    int ret = avcodec_send_packet(codecCtx, packet);
                    if (ret < 0) break;
                    while (avcodec_receive_frame(codecCtx, frame) >= 0) {
                        int samples = frame.nb_samples();

                        //更新进度
                        playedSamples += samples;
                        currentTime = (double) playedSamples / sampleRate;

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
                                logger.warn("不支持的样本格式: {}", fmtName.getString());
                                break mainloop;
                            }
                        }
                        av_frame_unref(frame);
                    }
                }
                av_packet_unref(packet);
            }
            logger.trace("释放资源");
            //av_free(fmtName);
            if (resample != null)
                resample.free();
            avformat_close_input(fmtCtx);
            avformat_free_context(fmtCtx);
            avcodec_free_context(codecCtx);
            av_frame_free(frame);
            av_packet_free(packet);
            fmtCtx.deallocate();
            codecCtx.deallocate();
            frame.deallocate();
            packet.deallocate();
            currentProgress++;
            if (event != null && CurrentStatus.is(CurrentStatus.Status.PLAYING))
                event.onFinish(currentProgress);
        }
        CurrentStatus.to(CurrentStatus.Status.STOP);
        currentProgress = 0;
        logger.trace("当前解码结束");
    }


    /**
     * 在新线程中解码
     */
    @Override
    public void start() {
        if (decode == null || decode.getState() == Thread.State.TERMINATED) {
            decode = new Thread(this);
            decode.setName("Decode Thread");
        }

        if (decode.getState() == Thread.State.NEW) {
            decode.start();
            logger.info("解码线程启动");
        }
    }


    /**
     * 获取当前歌曲播放的秒值
     * @return 秒值
     */
    public double getCurrentTimeSeconds() {
        return this.currentTime;
    }
}
