package org.balinhui.fpa.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.info.OutputInfo;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swresample.*;

public class Resample {
    private static final Logger logger = LogManager.getLogger(Resample.class);
    private final int dstChannels;
    private final int dstSampleFormat;
    private final int srcSampleRate, dstSampleRate;
    private int maxDstSamples = -1, dstSamples = -1;
    private final PointerPointer<BytePointer> dstData;
    private final IntPointer linSize;
    private final SwrContext swrCtx;
    private final int pointerSize;

    public Resample(
            int srcChannels, int srcSampleRate, int srcSampleFormat, OutputInfo info
    ) {
        this.srcSampleRate = srcSampleRate;
        this.dstChannels = info.channels;
        this.dstSampleRate = info.sampleRate;
        this.dstSampleFormat = info.sampleFormat;
        this.pointerSize = av_sample_fmt_is_planar(dstSampleFormat) == 1 ? dstChannels : 1;
        dstData = new PointerPointer<>(pointerSize);
        for (int i = 0; i < pointerSize; i++) {
            dstData.put(i, new BytePointer());
        }
        linSize = new IntPointer();
        AVChannelLayout srcLayout = new AVChannelLayout().nb_channels(srcChannels);
        AVChannelLayout dstLayout = new AVChannelLayout().nb_channels(dstChannels);
        swrCtx = swr_alloc();
        if (swrCtx == null) {
            logger.fatal("SwrContext分配失败");
            throw new RuntimeException("SwrContext分配失败");
        }
        int ret = swr_alloc_set_opts2(
                swrCtx,
                dstLayout, dstSampleFormat, dstSampleRate,
                srcLayout, srcSampleFormat, srcSampleRate,
                0, null
        );
        if (ret < 0) {
            logger.fatal("设置选项失败");
            throw new RuntimeException("设置选项失败");
        }
        ret = swr_init(swrCtx);
        if (ret < 0) {
            logger.fatal("SwrContext初始化失败");
            throw new RuntimeException("SwrContext初始化失败");
        }
    }

    public int getPointerSize() {
        return pointerSize;
    }

    public synchronized int process(BytePointer[] rawData, int samples, PointerPointer<?> srcData) {
        int ret;
        if (dstSamples == -1) {
            dstSamples = (int) av_rescale_rnd(samples, dstSampleRate, srcSampleRate, AV_ROUND_UP);
            maxDstSamples = dstSamples;
            ret = av_samples_alloc_array_and_samples(dstData, linSize, dstChannels, dstSamples, dstSampleFormat, 0);
            if (ret < 0) {
                logger.fatal("初始分配内存失败");
                throw new RuntimeException("分配内存失败");
            }
        }
        dstSamples = (int) av_rescale_rnd(swr_get_delay(swrCtx, srcSampleRate) + samples, dstSampleRate, srcSampleRate, AV_ROUND_UP);
        if (dstSamples > maxDstSamples) {
            for (int i = 0; i < pointerSize; i++) {
                if (dstData.get(i) != null) {
                    freePointer(dstData.get(i));
                    dstData.put(i, new BytePointer());
                }
            }
            ret = av_samples_alloc(dstData, linSize, dstChannels, dstSamples, dstSampleFormat, 1);
            if (ret < 0) {
                logger.fatal("分配内存失败");
                throw new RuntimeException("分配内存失败");
            }
            maxDstSamples = dstSamples;
        }
        int newSamples = swr_convert(swrCtx, dstData, dstSamples, srcData, samples);
        int bufferSize = av_samples_get_buffer_size(linSize, dstChannels, newSamples, dstSampleFormat, 1);
        for (int i = 0; i < pointerSize; i++) {
            Pointer p = dstData.get(i).position(0).limit(bufferSize);
            rawData[i] = new BytePointer(p);
        }
        return newSamples;
    }

    public synchronized void free() {
        for (int i = 0; i < pointerSize; i++) {
            if (dstData.get(i) != null) {
                freePointer(dstData.get(i));
                dstData.get(i).deallocate();
            }
        }
        av_free(linSize);
        swr_free(swrCtx);
        linSize.deallocate();//可能会导致崩溃
        swrCtx.deallocate();
        dstSamples = -1;
        maxDstSamples = -1;
        logger.trace("释放重采样资源");
    }

    private void freePointer(Pointer pointer) {
        PointerPointer<BytePointer> clear = new PointerPointer<>(1);
        clear.put(0, pointer);
        av_freep(clear);
    }
}
