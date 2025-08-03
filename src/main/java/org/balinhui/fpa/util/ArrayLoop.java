package org.balinhui.fpa.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayLoop {
    private static final int LIST_SIZE = 50;
    private static final Map<Class<?>, List<?>> arrays = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized static<T> T getArray(int size, Class<T> cls) {
        if (!cls.isArray()) throw new RuntimeException("Class is not array");

        boolean exist = arrays.containsKey(cls);
        List<T> arr = (List<T>) arrays.computeIfAbsent(cls, t -> new ArrayList<T>());

        if (!arr.isEmpty()) {
            return arr.removeFirst();
        }
        if (exist) throw new RuntimeException("Arrays exists, created repeatedly");
        for (int i = 0; i < LIST_SIZE; i++) {
            T array = (T) Array.newInstance(cls.getComponentType(), size);
            arr.add(i, array);
        }
        return arr.removeFirst();
    }

    @SuppressWarnings("unchecked")
    public synchronized static<T> void returnArray(T array) {
        if (array == null) return;
        Class<T> cls = (Class<T>) array.getClass();
        if (!cls.isArray()) throw new RuntimeException("It is not array");

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
}
