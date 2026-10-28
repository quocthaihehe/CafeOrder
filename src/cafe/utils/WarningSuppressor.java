package cafe.utils;

import java.io.OutputStream;
import java.io.PrintStream;

public class WarningSuppressor {
    private static volatile boolean initialized = false;

    /**
     * Tắt toàn bộ các cảnh báo đỏ vô hại của JavaFX (unnamed module)
     * và cảnh báo HotSpot JVM (sun.misc.Unsafe deprecation).
     */
    public static synchronized void suppress() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Bộ lọc Stream loại bỏ các cảnh báo JVM Unsafe / JavaFX module ra console (System.err)
        try {
            final PrintStream originalErr = System.err;
            System.setErr(new PrintStream(new OutputStream() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public void write(int b) {
                    if (b == '\n') {
                        String line = buffer.toString();
                        buffer.setLength(0);
                        if (!isIgnored(line)) {
                            originalErr.println(line);
                        }
                    } else if (b != '\r') {
                        buffer.append((char) b);
                    }
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    for (int i = off; i < off + len; i++) {
                        write(b[i]);
                    }
                }

                private boolean isIgnored(String line) {
                    return line.contains("WARNING: Unsupported JavaFX configuration") ||
                           line.contains("PlatformImpl startup") ||
                           line.contains("sun.misc.Unsafe") ||
                           line.contains("com.sun.marlin.OffHeapArray") ||
                           line.contains("Please consider reporting this to the maintainers") ||
                           line.contains("will be removed in a future release") ||
                           line.contains("terminally deprecated method");
                }
            }, true));
        } catch (Throwable ignored) {}
    }
}
