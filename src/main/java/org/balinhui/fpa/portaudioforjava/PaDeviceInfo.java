package org.balinhui.fpa.portaudioforjava;

import com.sun.jna.Structure;

@Structure.FieldOrder({"structVersion", "name", "hostApi", "maxInputChannels", "maxOutputChannels", "defaultLowInputLatency", "defaultLowOutputLatency", "defaultHighInputLatency", "defaultHighOutputLatency", "defaultSampleRate"})
public class PaDeviceInfo extends Structure {
    public int structVersion;
    public String name;
    public int hostApi;
    public int maxInputChannels;
    public int maxOutputChannels;
    public double defaultLowInputLatency;
    public double defaultLowOutputLatency;
    public double defaultHighInputLatency;
    public double defaultHighOutputLatency;
    public double defaultSampleRate;

    @Override
    public String toString() {
        return "PaDeviceInfo{" +
                "structVersion=" + structVersion +
                ", name='" + name + '\'' +
                ", hostApi=" + hostApi +
                ", maxInputChannels=" + maxInputChannels +
                ", maxOutputChannels=" + maxOutputChannels +
                ", defaultLowInputLatency=" + defaultLowInputLatency +
                ", defaultLowOutputLatency=" + defaultLowOutputLatency +
                ", defaultHighInputLatency=" + defaultHighInputLatency +
                ", defaultHighOutputLatency=" + defaultHighOutputLatency +
                ", defaultSampleRate=" + defaultSampleRate +
                '}';
    }
}
