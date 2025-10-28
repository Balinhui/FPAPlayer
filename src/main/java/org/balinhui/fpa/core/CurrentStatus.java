package org.balinhui.fpa.core;

import java.util.concurrent.atomic.AtomicReference;

public class CurrentStatus {
    /**
     * 储存当前的运行状态，种类在Status枚举中
     */
    private static final AtomicReference<Status> currentStatus = new AtomicReference<>(Status.STOP);

    /**
     * 查询是否为输入的状态
     * @param status 需要查询的状态
     * @return 相同-true | 不相同-false
     */
    public static synchronized boolean is(Status status) {
        return currentStatus.get() == status;
    }

    /**
     * 将当前状态设置为输入的状态（请勿随意调用）
     * @param status 需要设置的状态
     */
    public static synchronized void to(Status status) {
        currentStatus.set(status);
        CurrentStatus.class.notifyAll();//如果之前因修改状态使歌曲暂停，此可以在取消暂停时唤醒所有线程
    }

    /**
     * 检查状态设置是否为PAUSE，如果是，则将线程挂起
     * @return 是否暂停过
     * @throws InterruptedException 当挂起线程失败时抛出，一般不去管它（暂停失败不会再按一次？）
     */
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
