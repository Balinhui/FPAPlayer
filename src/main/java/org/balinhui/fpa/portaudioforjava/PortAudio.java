package org.balinhui.fpa.portaudioforjava;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public interface PortAudio extends Library {
    /**
     * If the return value of the string has Chinese characters, and it is encoded in UTF-8,
     * invoke it for a value without garbled characters.
     */
    static void setUTF_8() {
        System.setProperty("jna.encoding", "UTF-8");
    }
    PortAudio INSTANCE = Native.load("portaudio_x64", PortAudio.class);

    int paNoError = 0;
    int paNoDevice = -1;
    int paFormatIsSupported = 0;//false
    int paFramesPerBufferUnspecified = 0;


    String Pa_GetVersionText();

    String Pa_GetErrorText(int errCode);

    int Pa_Initialize();

    int Pa_Terminate();

    int Pa_GetDeviceCount();

    int Pa_GetDefaultOutputDevice();

    PaDeviceInfo Pa_GetDeviceInfo(int device);

    int Pa_IsFormatSupported(PaStreamParameters inputParameters, PaStreamParameters outputParameters, double sampleRate);

    int Pa_OpenStream(PointerByReference stream, PaStreamParameters inputParameters, PaStreamParameters outputParameters, double sampleRate, long framesPerBuffer, long streamFlags, PaStreamCallback streamCallback, Pointer userData);

    int Pa_CloseStream(PaStream stream);

    int Pa_StartStream(PaStream stream);

    int Pa_StopStream(PaStream stream);

    boolean Pa_IsStreamStopped(PaStream stream);

    boolean Pa_IsStreamActive(PaStream stream);

    double Pa_GetStreamTime(PaStream stream);

    double Pa_GetStreamCpuLoad(PaStream stream);

    int Pa_WriteStream(PaStream stream, float[] buffer, long frames);

    int Pa_WriteStream(PaStream stream, short[] buffer, long frames);

    long Pa_GetStreamWriteAvailable(PaStream stream);

    int Pa_GetSampleSize(long format);

    void Pa_Sleep(long msec);
}
