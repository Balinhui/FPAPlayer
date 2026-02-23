package org.balinhui.fpa.portaudioforjava;

public interface PaSampleFormat {
    long paFloat32 = 0x00000001L;
    long paInt32 = 0x00000002L;
    long paInt24 = 0x00000004L;
    long paInt16 = 0x00000008L;
    long paInt8 = 0x00000010L;
    long paUInt8 = 0x00000020L;
    long paCustomFormat = 0x00010000L;
    long paNonInterleaved = 0x80000000L;
}
