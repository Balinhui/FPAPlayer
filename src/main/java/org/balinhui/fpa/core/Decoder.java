package org.balinhui.fpa.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.FPAScreen;
import org.balinhui.fpa.info.AudioInfo;
import org.balinhui.fpa.info.OutputInfo;
import org.balinhui.fpa.nativeapis.Global;
import org.balinhui.fpa.nativeapis.MessageFlags;
import org.balinhui.fpa.util.*;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.ShortPointer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

/**
 * 用于解码音频，采用单例对象。依赖由bytedeco的JavaCPP绑定的FFmpeg实现，
 * 需使用{@code read()}读取音频信息，并通过{@code start}启动解码线程。
 * 读取和解码分别使用不同的{@code AVFormatContext}对象，防止在高并发中
 * 资源的访问出错。解码完成后的数据，将会放入缓冲区中{@link Buffer}，供
 * 播放线程使用。
 */
public class Decoder implements Runnable, AudioHandler {
    private static final Logger logger = LogManager.getLogger(Decoder.class);
    private String[] paths;
    private final Buffer buffer = new Buffer();
    private static final Decoder decoder = new Decoder();
    private OutputInfo outputInfo;
    private int currentProgress = 0;//当路径中有多首歌时，用于指示播放进度
    private FinishEvent event;//解码完一首歌后调用
    private Thread decode;//解码线程

    public static Decoder getDecoder() {
        return decoder;
    }

    private Decoder() {
        try (BytePointer versionPointer = av_version_info()) {
            logger.info("当前FFmpeg版本: {}", versionPointer.getString());
        }
        av_log_set_level(AV_LOG_ERROR);
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
                throw new RuntimeException("Open file failed");
            }
            logger.trace("打开文件");
            if (avformat_find_stream_info(fmtCtx, (PointerPointer<?>) null) < 0) {
                logger.error("Cant find stream info");
                throw new RuntimeException("Cant find stream info");
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
                logger.error("没有找到解码器参数，文件格式可能不符");
                //throw new RuntimeException("Doesn't find codec parameter");
                Global.message(
                        Win32.getLongHWND(FPAScreen.mainWindow),
                        "错误的文件类型!",
                        "请选择音频文件!!!",
                        MessageFlags.DisplayButtons.OK | MessageFlags.Icons.ERROR
                );
                return null;
            }
            logger.trace("找到解码器参数");
            byte[] coverData = null;
            if (coverStream == -1) {
                try {
                    logger.info("ffmpeg无法找到封面，尝试通过文件提取");
                    coverData = FlacCoverExtractor.extractFlacCover(path);
                } catch (IOException e) {
                    logger.fatal(e.getMessage());
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
                    fmtCtx.duration() / (float) AV_TIME_BASE,
                    av_sample_fmt_is_planar(codecPar.format()) == 1
            );
        } finally {
            avformat_close_input(fmtCtx);
            avformat_free_context(fmtCtx);
            fmtCtx.deallocate();
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
            if (codecCtx.isNull()) {
                logger.fatal("Doest allocate context");
                throw new RuntimeException("Doest allocate context");
            }
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
            } else {
                dstChannels = srcChannels;
                dstSampleFormat = srcSampleFormat;
            }

            //歌曲部分信息
            String fmtName = AudioUtil.getSampleFormatName(dstSampleFormat);
            logger.trace("歌曲样本格式: {} ", fmtName);

            AVPacket packet = av_packet_alloc();
            AVFrame frame = av_frame_alloc();
            if (packet.isNull() || frame.isNull()) {
                logger.fatal("Cant allocate packet or frame");
                throw new RuntimeException("Cant allocate packet or frame");
            }
            BytePointer[] rawData = new BytePointer[1];
            mainloop:
            while (!CurrentStatus.is(CurrentStatus.Status.STOP)) {

                if (av_read_frame(fmtCtx, packet) < 0)
                    break;
                if (packet.stream_index() == streamIndex) {
                    int ret = avcodec_send_packet(codecCtx, packet);
                    if (ret < 0)
                        break;
                    while (true) {
                        ret = avcodec_receive_frame(codecCtx, frame);
                        if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF)
                            break;
                        else if (ret < 0) {
                            logger.error("解码出错或者结束解码");
                            break mainloop;
                        }

                        int samples = frame.nb_samples();
                        int oldSamples = samples;

                        if (needsResample) {
                            samples = resample.process(rawData, samples, frame.data());
                        } else {
                            rawData[0] = frame.data(0);
                        }
                        int arraySize = samples * dstChannels;

                        switch (dstSampleFormat) {
                            case AV_SAMPLE_FMT_S16 -> {
                                short[] shortData = ArrayLoop.getArray(arraySize, short[].class);
                                shortData = ArrayLoop.reSize(shortData, arraySize);
                                ShortPointer data = new ShortPointer(rawData[0]);
                                data.get(shortData);
                                buffer.put(Buffer.Data.of(samples, oldSamples, shortData));
                            }
                            case AV_SAMPLE_FMT_FLT -> {
                                float[] floatData = ArrayLoop.getArray(arraySize, float[].class);
                                floatData = ArrayLoop.reSize(floatData, arraySize);
                                FloatPointer data = new FloatPointer(rawData[0]);
                                data.get(floatData);
                                buffer.put(Buffer.Data.of(samples, oldSamples, floatData));
                            }
                        }
                        // avcodec_receive_frame() 不会分配新的内存，所以不用调用
                        // av_frame_unref(frame);
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
            currentProgress++;
            if (event != null && CurrentStatus.is(CurrentStatus.Status.PLAYING)) {
                //向播放器发送结束信息
                buffer.put(Buffer.Data.of(currentProgress));
                //对下一首歌预读
                event.onFinish(currentProgress);
            } else if (CurrentStatus.is(CurrentStatus.Status.STOP)) {
                buffer.clear();
                logger.info("强制退出，清空缓冲区");
            }
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
}
