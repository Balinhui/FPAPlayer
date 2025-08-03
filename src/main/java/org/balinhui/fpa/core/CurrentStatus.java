package org.balinhui.fpa.core;

public class CurrentStatus {
    public static Status currentStatus = Status.STOP;

    public enum Status{
        PLAYING, STOP
    }
}
