package org.balinhui.fpa.core;

public class CurrentStatus {
    private static volatile Status currentStatus = Status.STOP;

    public static synchronized boolean is(Status status) {
        return currentStatus == status;
    }

    public static synchronized void to(Status status) {
        currentStatus = status;
    }

    public enum Status {
        /**
         * 用于指示播放开始
         */
        PLAYING,

        /**
         * 用于指示解码完成
         */
        STOP
    }
}
