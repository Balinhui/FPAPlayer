package org.balinhui.fpa.info;

public class OutputInfo {
    public boolean resample;
    public int channels;
    public int sampleRate;
    public int sampleFormat;

    public OutputInfo(boolean resample, int channels, int sampleRate, int sampleFormat) {
        this.resample = resample;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.sampleFormat = sampleFormat;
    }
}
