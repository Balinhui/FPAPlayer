package org.balinhui.fpa.core;

import java.util.concurrent.atomic.AtomicReference;

public class CurrentStatus {
    private static final AtomicReference<Status> currentStatus = new AtomicReference<>(Status.STOP);

    public static synchronized boolean is(Status status) {
        return currentStatus.get() == status;
    }

    public static synchronized void to(Status status) {
        currentStatus.set(status);
    }

    public enum Status {
        /**
         * 用于指示播放开始
         */
        PLAYING,

        /**
         * 用于指示解码完成
         */
        STOP,

        /**
         * 暂时还没有用，等待解决歌词问题
         */
        PAUSE
    }
}
