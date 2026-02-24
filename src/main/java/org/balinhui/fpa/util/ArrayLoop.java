package org.balinhui.fpa.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 数组池，实现数组和内存的循环使用
 */
public class ArrayLoop {
    private static final Logger logger = LogManager.getLogger(ArrayLoop.class);
    private static final int LIST_SIZE = 50;
    private static final Map<Class<?>, List<?>> arrays = new HashMap<>();

    private ArrayLoop() {}

    /**
     * 向数组池申请数组
     * 如果请求的类型在<code>arrays</code>中存在，则会将<code>arrays</code>中对应类型的数组集合的第一个数组返回，并从数组集合中去除<br>
     * 如果没有，则会创建一个为参数cls类型的数组集合，默认大小为<code>LIST_SIZE = 50</code>个，然后返回第一个数组，再去除
     * <br>
     * 重要！！！由于HashMap和ArrayList是非线程安全的，所以此方法已加锁
     *
     * @param size 申请数组的大小
     * @param cls 申请数组的类型
     * @return 申请的数组
     * @param <T> float[] 或 short[]
     */
    @SuppressWarnings("unchecked")
    public synchronized static<T> T getArray(int size, Class<T> cls) {
        //检测传入的类型是否为数组类型
        if (!cls.isArray()) {
            logger.fatal("传入类型不是数组");
            throw new RuntimeException("Class is not array");
        }

        //检查arrays中是否有cls对应类型的数组集合
        boolean exist = arrays.containsKey(cls);
        //获取arrays中cls对应的数组集合，如果没有，则创建一个
        List<T> arr = (List<T>) arrays.computeIfAbsent(cls, t -> new ArrayList<T>());

        //判断获取的数组集合是否为空，如果不为空，则说明早已创建，直接返回数组集合中的第一个，同时忽略了size参数，数组大小若不符合，则需后来修改
        //如果为空，则说明此类型的数组集合是刚刚创建，需要在里面添加新的数组
        if (!arr.isEmpty()) {
            return arr.removeFirst();
        }
        if (exist) {
            logger.fatal("内容已存在，正在重复创建");
            throw new RuntimeException("Arrays exists, created repeatedly");
        }
        for (int i = 0; i < LIST_SIZE; i++) {
            //通过循环添加数组，大小默认为传入的size，其它后来使用的数组大小若不符合，则需自行修改
            T array = (T) Array.newInstance(cls.getComponentType(), size);
            arr.add(i, array);
        }
        return arr.removeFirst();
    }

    /**
     * 返还使用后的数组，将内存重复使用，在getArray()方法中去除了给出的数组，在这个方法中将添加回来，形成循环
     * <br>
     * 重要！！！由于HashMap和ArrayList是非线程安全的，所以此方法已加锁
     *
     * @param array 使用完返回的数组
     * @param <T> float[] 或 short[]
     */
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

        //检查arrays中是否有对应的数组集合，有则将数组添加进去，无则新建一个再添加
        //这样可能会导致arrays中多出很多使用量很少的奇怪类型数组，但考虑到没有其他地方使用此方法，这样也没有什么问题
        List<T> list = (List<T>) arrays.computeIfAbsent(cls, t -> new ArrayList<T>());
        list.add(array);
    }

    /**
     * 将大小不符合的short数组重新分配大小<br>
     * 对于要求的新数组容量小于旧数组容量，则将旧数组多出的部分填充为0.
     * @param data 需要重新分配大小的short数组
     * @param newSize 新的大小
     * @return 重新分配后的数组
     */
    public static short[] reSize(short[] data, int newSize) {
        if (data.length == newSize) return data;
        else if (data.length > newSize) {
            Arrays.fill(data, newSize, data.length, (short) 0);
            return data;
        }
        return new short[newSize];
    }

    /**
     * 将大小不符合的float数组重新分配大小<br>
     * 对于要求的新数组容量小于旧数组容量，则将旧数组多出的部分填充为0.
     * @param data 需要重新分配大小的float数组
     * @param newSize 新的大小
     * @return 重新分配后的数组
     */
    public static float[] reSize(float[] data, int newSize) {
        if (data.length == newSize) return data;
        else if (data.length > newSize) {
            Arrays.fill(data, newSize, data.length, 0F);
            return data;
        }
        return new float[newSize];
    }

    /**
     * 将arrays中的内容清空<br>
     * 一般用于初始化，播放结束，切换等场景
     */
    public static void clear() {
        arrays.clear();
        logger.trace("清空数组集合");
    }
}
