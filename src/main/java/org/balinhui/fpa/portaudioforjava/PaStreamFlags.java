package org.balinhui.fpa.portaudioforjava;

public interface PaStreamFlags {
    long paNoFlag = 0L;
    long paClipOff = 0x00000001L;
    long paDitherOff = 0x00000002L;
    long paNeverDropInput = 0x00000004L;
    long paPrimeOutputBuffersUsingStreamCallback = 0x00000008L;
    long paPlatformSpecificFlags = 0xFFFF0000L;
}
