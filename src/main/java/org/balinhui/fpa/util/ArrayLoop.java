package org.balinhui.fpa.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayLoop {
    private static final Logger logger = LogManager.getLogger(ArrayLoop.class);
    private static final int LIST_SIZE = 50;
    private static final Map<Class<?>, List<?>> arrays = new HashMap<>();

    private ArrayLoop() {}

    @SuppressWarnings("unchecked")
    public synchronized static<T> T getArray(int size, Class<T> cls) {
        if (!cls.isArray()) {
            logger.fatal("传入类型不是数组");
            throw new RuntimeException("Class is not array");
        }

        boolean exist = arrays.containsKey(cls);
        List<T> arr = (List<T>) arrays.computeIfAbsent(cls, t -> new ArrayList<T>());

        if (!arr.isEmpty()) {
            return arr.removeFirst();
        }
        if (exist) {
            logger.fatal("内容已存在，正在重复创建");
            throw new RuntimeException("Arrays exists, created repeatedly");
        }
        for (int i = 0; i < LIST_SIZE; i++) {
            T array = (T) Array.newInstance(cls.getComponentType(), size);
            arr.add(i, array);
        }
        return arr.removeFirst();
    }

    @SuppressWarnings("unchecked")
    public synchronized static<T> void returnArray(T array) {
        if (array == null) {
            logger.warn("回收数组为null");
            return;
        }
        Class<T> cls = (Class<T>) array.getClass();
        if (!cls.isArray()) {
            logger.fatal("数据类型不是数组");
            throw new RuntimeException("It is not array");
        }

        List<T> list = (List<T>) arrays.computeIfAbsent(cls, t -> new ArrayList<T>());
        list.add(array);
    }

    public synchronized static byte[] reSize(byte[] data, int newSize) {
        if (data.length == newSize) return data;
        return new byte[newSize];
    }

    public synchronized static short[] reSize(short[] data, int newSize) {
        if (data.length == newSize) return data;
        return new short[newSize];
    }

    public synchronized static int[] reSize(int[] data, int newSize) {
        if (data.length == newSize) return data;
        return new int[newSize];
    }

    public synchronized static float[] reSize(float[] data, int newSize) {
        if (data.length == newSize) return data;
        return new float[newSize];
    }

    public static void clear() {
        arrays.clear();
        logger.trace("清空集合");
    }
}
