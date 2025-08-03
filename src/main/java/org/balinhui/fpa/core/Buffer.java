package org.balinhui.fpa.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Buffer {
    private static final int QUEUE_SIZE = 40;
    private static final BlockingQueue<Data<?>> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);

    public void put(Data<?> data) {
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Data<?> take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
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
        private final int nb_samples;
        private final T data;
        private final DataType type;

        private Data(int nb_samples, T data, DataType type) {
            this.nb_samples = nb_samples;
            this.data = data;
            this.type = type;
        }

        public int getNb_samples() {
            return nb_samples;
        }

        public T getData() {
            return data;
        }

        public DataType getType() {
            return type;
        }

        public static Data<byte[]> of(int nb_samples, byte[] data) {
            return new Data<>(nb_samples, data, DataType.BYTE);
        }

        public static Data<short[]> of(int nb_samples, short[] data) {
            return new Data<>(nb_samples, data, DataType.SHORT);
        }

        public static Data<int[]> of(int nb_samples, int[] data) {
            return new Data<>(nb_samples, data, DataType.INTEGER);
        }

        public static Data<float[]> of(int nb_samples, float[] data) {
            return new Data<>(nb_samples, data, DataType.FLOAT);
        }
    }

    public enum DataType {
        BYTE, SHORT, INTEGER, FLOAT
    }
}
