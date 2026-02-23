package org.balinhui.fpa.portaudioforjava;

import com.sun.jna.PointerType;
import com.sun.jna.ptr.PointerByReference;

public class PaStream extends PointerType {
    public PaStream() {super();}
    public PaStream(PointerByReference p) {super(p.getValue());}
}
