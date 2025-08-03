module org.balinhui.fpa {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.bytedeco.ffmpeg;

    requires PortAudio;
    requires org.jetbrains.annotations;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    opens org.balinhui.fpa to javafx.fxml;
    opens org.balinhui.fpa.ui to com.sun.jna;
    exports org.balinhui.fpa;
}