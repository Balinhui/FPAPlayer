package org.balinhui.fpa.portaudioforjava;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

@Structure.FieldOrder({"device", "channelCount", "sampleFormat", "suggestedLatency", "hostApiSpecificStreamInfo"})
public class PaStreamParameters extends Structure {
    public int device;
    public int channelCount;
    public long sampleFormat;
    public double suggestedLatency;
    public Pointer hostApiSpecificStreamInfo;

    @Override
    public String toString() {
        return "PaStreamParameters{" +
                "device=" + device +
                ", channelCount=" + channelCount +
                ", sampleFormat=" + sampleFormat +
                ", suggestedLatency=" + suggestedLatency +
                ", hostApiSpecificStreamInfo=" + hostApiSpecificStreamInfo +
                '}';
    }
}
