package org.balinhui.fpa.portaudioforjava;

import com.sun.jna.Structure;

@Structure.FieldOrder({"inputBufferAdcTime", "currentTime", "outputBufferDacTime"})
public class PaStreamCallbackTimeInfo extends Structure {
    public double inputBufferAdcTime;
    public double currentTime;
    public double outputBufferDacTime;

    @Override
    public String toString() {
        return "PaStreamCallbackTimeInfo{" +
                "inputBufferAdcTime=" + inputBufferAdcTime +
                ", currentTime=" + currentTime +
                ", outputBufferDacTime=" + outputBufferDacTime +
                '}';
    }
}
