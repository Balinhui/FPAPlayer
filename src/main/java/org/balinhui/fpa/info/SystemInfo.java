package org.balinhui.fpa.info;

public class SystemInfo {
    public final Name name;
    public final Arch arch;
    public final double version;
    public static SystemInfo systemInfo;

    private SystemInfo(Name name, Arch arch, double version) {
        this.name = name;
        this.arch = arch;
        this.version = version;
    }

    public static void read(String name, String arch, String version) {
        if (systemInfo == null) {
            Name eName = Name.UNKNOWN;
            Arch eArch = Arch.UNKNOWN;
            if (name.toLowerCase().contains("windows")) eName = Name.WINDOWS;
            if (name.toLowerCase().contains("mac")) eName = Name.MACOS;
            if (name.toLowerCase().contains("linux")) eName = Name.LINUX;

            if (arch.startsWith("amd64")) eArch = Arch.AMD64;
            if (arch.startsWith("x86")) eArch = Arch.X86;
            if (arch.startsWith("aarch64")) eArch = Arch.AARCH64;
            if (arch.startsWith("arm")) eArch = Arch.ARM;
            systemInfo = new SystemInfo(eName, eArch, Double.parseDouble(version));
        }
    }

    public enum Name {
        WINDOWS, MACOS, LINUX, UNKNOWN
    }

    public enum Arch {
        //x86_64
        AMD64,
        //x86_32
        X86,
        //arm64
        AARCH64,
        //arm32
        ARM,
        UNKNOWN
    }
}
