package org.balinhui.fpa.nativeapis;

/**
 * 用于Global中message方法的type参数
 */
@SuppressWarnings("all")
public class MessageFlags {
    public static class DisplayButtons {
        public static final long ABORT_RETRY_IGNORE = 0x00000002L;
        public static final long CANCEL_TRY_CONTINUE = 0x00000006L;
        public static final long HELP = 0x00004000L;
        public static final long OK = 0x00000000L;
        public static final long OK_CANCEL = 0x00000001L;
        public static final long RETRY_CANCEL = 0x00000005L;
        public static final long YES_NO = 0x00000004L;
        public static final long YES_NO_CANCEL = 0x00000003L;
    }

    public static class Icons {
        public static final long EXCLAMATION = 0x00000030L;
        public static final long WARNING = 0x00000030L;
        public static final long INFORMATION = 0x00000040L;
        public static final long ASTERISK = 0x00000040L;
        public static final long QUESTION = 0x00000020L;
        public static final long STOP = 0x00000010L;
        public static final long ERROR = 0x00000010L;
        public static final long HAND = 0x00000010L;
    }

    public static class DefaultButtons {
        public static final long DEFBUTTON1 = 0x00000000L;
        public static final long DEFBUTTON2 = 0x00000100L;
        public static final long DEFBUTTON3 = 0x00000200L;
        public static final long DEFBUTTON4 = 0x00000300L;
    }

    public static class DialogForms {
        public static final long APPL_MODAL = 0x00000000L;
        public static final long SYSTEM_MODAL = 0x00001000L;
        public static final long TASK_MODAL = 0x00002000L;
    }

    public static class Others {
        public static final long DEFAULT_DESKTOP_ONLY = 0x00020000L;
        public static final long RIGHT = 0x00080000L;
        public static final long RTLREADING = 0x00100000L;
        public static final long SETFOREGROUND = 0x00010000L;
        public static final long TOPMOST = 0x00040000L;
        public static final long SERVICE_NOTIFICATION = 0x00200000L;
    }

    /**
     * Global中message方法的返回值
     */
    public static class ReturnValue {
        public static final int ABORT = 3;
        public static final int CANCEL = 2;
        public static final int CONTINUE = 11;
        public static final int IGNORE = 5;
        public static final int NO = 7;
        public static final int OK = 1;
        public static final int RETRY = 4;
        public static final int TRYAGAIN = 10;
        public static final int YES = 6;
    }
}
