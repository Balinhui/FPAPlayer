package org.balinhui.fpa.util;

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
        swr_alloc_set_opts2(
                swrCtx,
                dstLayout, dstSampleFormat, dstSampleRate,
                srcLayout, srcSampleFormat, srcSampleRate,
                0, null
        );
        swr_init(swrCtx);
    }

    public int getPointerSize() {
        return pointerSize;
    }

    public synchronized int process(BytePointer[] rawData, int samples, PointerPointer<?> srcData) {
        if (dstSamples == -1) {
            dstSamples = (int) av_rescale_rnd(samples, dstSampleRate, srcSampleRate, AV_ROUND_UP);
            maxDstSamples = dstSamples;
            av_samples_alloc_array_and_samples(dstData, linSize, dstChannels, dstSamples, dstSampleFormat, 0);
        }
        dstSamples = (int) av_rescale_rnd(swr_get_delay(swrCtx, srcSampleRate) + samples, dstSampleRate, srcSampleRate, AV_ROUND_UP);
        if (dstSamples > maxDstSamples) {
            for (int i = 0; i < pointerSize; i++) {
                if (dstData.get(i) != null) {
                    freePointer(dstData.get(i));
                    dstData.put(i, new BytePointer());
                }
            }
            av_samples_alloc(dstData, linSize, dstChannels, dstSamples, dstSampleFormat, 1);
            maxDstSamples = dstSamples;
        }
        int newSamples = swr_convert(swrCtx, dstData, dstSamples, srcData, samples);
        int bufferSize = av_samples_get_buffer_size(linSize, dstChannels, newSamples, dstSampleFormat, 1);
        for (int i = 0; i < pointerSize; i++) {
            rawData[i] = new BytePointer(dstData.get(i).position(0).limit(bufferSize));
        }
        return newSamples;
    }

    public synchronized void free() {
        for (int i = 0; i < pointerSize; i++) {
            if (dstData.get(i) != null) freePointer(dstData.get(i));
        }
        av_free(linSize);
        swr_free(swrCtx);
        dstSamples = -1;
        maxDstSamples = -1;
    }

    private void freePointer(Pointer pointer) {
        PointerPointer<BytePointer> clear = new PointerPointer<>(1);
        clear.put(0, pointer);
        av_freep(clear);
    }
}
