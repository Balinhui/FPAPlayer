package org.balinhui.fpa.portaudioforjava;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface PaStreamCallback extends Callback {
    int invoke(
            Pointer input,
            Pointer output,
            long frameCount,
            PaStreamCallbackTimeInfo timeInfo,
            long statusFlags,
            Pointer userData
    );
}
