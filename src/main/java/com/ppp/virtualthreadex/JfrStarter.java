// FILE: src/main/java/com/ppp/virtualthreadex/JfrStarter.java
package com.ppp.virtualthreadex;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JFR starter that:
 * - Enables useful events (VirtualThread, sockets, monitors)
 * - Flushes BEFORE stop (prevents "No chunks")
 * - Retries dump once if needed
 */
public final class JfrStarter {
    private static volatile Recording recording;

    private JfrStarter() {}

    public static void startRecording(String baseName) {
        try {
            Files.createDirectories(Path.of("jfr"));
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path out = Path.of("jfr", baseName + "-" + ts + ".jfr");

            var cfg = Configuration.getConfiguration("profile"); // rich default
            var r = new Recording(cfg);
            r.setToDisk(true);
            r.setMaxAge(Duration.ofMinutes(10)); // optional rolling window

            // Be explicit (profile usually enables these already)
            r.enable("jdk.VirtualThreadStart");
            r.enable("jdk.VirtualThreadEnd");
            r.enable("jdk.VirtualThreadPinned");
            r.enable("jdk.JavaMonitorEnter");
            r.enable("jdk.SocketRead");
            r.enable("jdk.SocketWrite");

            r.start();
            recording = r;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
//                    r.flush();       // ensure a chunk exists
                    r.stop();        // stop first
                    dumpWithRetry(r, out);
                    System.out.println("[JFR] Recording dumped to: " + out);
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    try { r.close(); } catch (Throwable ignore) {}
                }
            }, "jfr-dump-hook"));

            System.out.println("[JFR] Started (profile). Will dump to: " + out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start JFR", e);
        }
    }

    private static void dumpWithRetry(Recording r, Path out) throws Exception {
        try {
            r.dump(out);
        } catch (FileNotFoundException noChunks) {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
//            r.flush();
            r.dump(out);
        }
    }
}