package org.balinhui.fpa.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 歌曲缓冲区
 */
public class Buffer {
    private static final Logger logger = LogManager.getLogger(Buffer.class);
    private static final int QUEUE_SIZE = 40;
    private static final BlockingQueue<Data<?>> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);

    /**
     * 将数据填入缓存区
     * @param data 歌曲的数据
     */
    public void put(Data<?> data) {
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            logger.fatal("put失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 将数据取出缓冲区
     * @return 歌曲的数据
     */
    public Data<?> take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            logger.fatal("take失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 清空缓冲区
     */
    public void clear() {
        queue.clear();
    }

    /**
     * 缓冲区是否为空
     * @return 如果为空返回true， 如果不为空返回false
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * 内部类，歌曲数据
     * @param <T> float[]或short[]
     */
    public static class Data<T> {
        /**
         * 如果重采样，表示重采样后的采样数，一般用这个
         */
        public final int nb_samples;

        /**
         * 如果重采样，表示重采样前的采样数，用于计算播放进度
         */
        public final int old_samples;

        /**
         * 需要存入缓冲区的数据，同时也包含了歌曲解码完成后的进度信息
         */
        public final T data;
        public final DataType type;

        private Data(int nb_samples, int oldSamples, T data, DataType type) {
            this.nb_samples = nb_samples;
            this.old_samples = oldSamples;
            this.data = data;
            this.type = type;
        }

        /**
         * 创建类型为short的数据
         * @param nb_samples 新的采样数
         * @param oldSamples 旧采样数
         * @param data 数据
         * @return 包装好的数据类
         */
        public static Data<short[]> of(int nb_samples, int oldSamples, short[] data) {
            return new Data<>(nb_samples, oldSamples, data, DataType.SHORT);
        }

        /**
         * 创建类型为float的数据
         * @param nb_samples 新的采样数
         * @param oldSamples 旧采样数
         * @param data 数据
         * @return 包装好的数据类
         */
        public static Data<float[]> of(int nb_samples, int oldSamples, float[] data) {
            return new Data<>(nb_samples, oldSamples, data, DataType.FLOAT);
        }

        /**
         * 创建指示歌曲结束的数据标记
         * @param process 当前歌曲进度
         * @return 包装好的数据类
         */
        public static Data<Integer> of(int process) {
            return new Data<>(0, 0, process, null);
        }
    }

    public enum DataType {
        SHORT, FLOAT
    }
}
