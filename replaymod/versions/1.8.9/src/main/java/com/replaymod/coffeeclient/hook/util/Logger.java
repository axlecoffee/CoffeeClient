package com.replaymod.coffeeclient.hook.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static boolean enabled = true;
    private static File logFile;
    private static boolean headerWritten = false;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static void setLogFile(File file) {
        logFile = file;
    }

    public static void info(String msg) {
        if (!enabled) return;
        log("INFO", msg);
    }

    public static void warn(String msg) {
        if (!enabled) return;
        log("WARN", msg);
    }

    public static void error(String msg) {
        log("ERROR", msg);
    }

    public static void error(String msg, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        log("ERROR", msg + "\n" + sw);
    }

    private static synchronized void log(String level, String msg) {
        System.out.println("[CoffeeLoader] " + msg);
        if (logFile == null) return;
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileWriter fw = new FileWriter(logFile, true)) {
                if (!headerWritten) {
                    headerWritten = true;
                    fw.write("\n=== CoffeeLoader Herpful Log " + FMT.format(new Date()) + " ===\n");
                    fw.write("Java: " + System.getProperty("java.version") + "\n");
                    fw.write("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n\n");
                }
                fw.write("[" + FMT.format(new Date()) + "] [" + level + "] " + msg + "\n");
            }
        } catch (Throwable ignored) {}
    }
}
