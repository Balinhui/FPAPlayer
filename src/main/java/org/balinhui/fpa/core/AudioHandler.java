package org.balinhui.fpa.core;

/**
 * 专门用于解码器和播放器应对相同事件
 */
public interface AudioHandler {

    int NO_ARGS = -1;

    /**
     * 进行各自的主任务
     */
    void start();

    /**
     * 为解码器，播放器处理完后执行事件
     * @param event 要执行的回调
     */
    void setOnFinished(FinishEvent event);

    @FunctionalInterface
    interface FinishEvent {
        void onFinish(int args);
    }
}
