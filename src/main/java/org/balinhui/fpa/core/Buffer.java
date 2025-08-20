package org.balinhui.fpa.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Buffer {
    private static final Logger logger = LogManager.getLogger(Buffer.class);
    private static final int QUEUE_SIZE = 40;
    private static final BlockingQueue<Data<?>> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);

    public void put(Data<?> data) {
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            logger.fatal("put失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Data<?> take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            logger.fatal("take失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        queue.clear();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public static class Data<T> {
        public final int nb_samples;
        public final int old_samples;
        public final T data;
        public final DataType type;

        private Data(int nb_samples, int oldSamples, T data, DataType type) {
            this.nb_samples = nb_samples;
            this.old_samples = oldSamples;
            this.data = data;
            this.type = type;
        }

        public static Data<short[]> of(int nb_samples, int oldSamples, short[] data) {
            return new Data<>(nb_samples, oldSamples, data, DataType.SHORT);
        }

        public static Data<float[]> of(int nb_samples, int oldSamples, float[] data) {
            return new Data<>(nb_samples, oldSamples, data, DataType.FLOAT);
        }
    }

    public enum DataType {
        SHORT, FLOAT
    }
}
