module org.balinhui.fpa {
    requires javafx.controls;
    requires org.bytedeco.ffmpeg;

    requires PortAudio;
    requires org.jetbrains.annotations;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.apache.logging.log4j;
    requires javafx.graphics;
    requires org.apache.logging.log4j.core;

    exports org.balinhui.fpa to javafx.graphics;
    exports org.balinhui.fpa.util to com.sun.jna;
    exports org.balinhui.fpa.ui to com.sun.jna, javafx.graphics;
}