package org.balinhui.fpa.core;

import java.util.concurrent.atomic.AtomicReference;

public class CurrentStatus {
    private static final AtomicReference<Status> currentStatus = new AtomicReference<>(Status.STOP);

    public static synchronized boolean is(Status status) {
        return currentStatus.get() == status;
    }

    public static synchronized void to(Status status) {
        currentStatus.set(status);
        CurrentStatus.class.notifyAll();
    }

    public static synchronized boolean waitUntilNotPaused() throws InterruptedException {
        boolean flag = false;
        while (is(Status.PAUSE)) {
            flag = true;
            CurrentStatus.class.wait();
        }
        return flag;
    }

    public enum Status {
        /**
         * 用于指示播放开始和播放时
         */
        PLAYING,

        /**
         * 用于指示解码完成
         */
        STOP,

        /**
         * 只是暂停
         */
        PAUSE
    }
}
