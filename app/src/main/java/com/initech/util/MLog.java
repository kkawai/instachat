package com.initech.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MLog {

    private static boolean globalEnabled = true;

    private MLog() {
    }

    private static boolean isEnabled(String tag) {
        return globalEnabled;
    }

    public static void i(String tag, Object... vals) {
        if (isEnabled(tag)) {
            Logger.getLogger(tag).log(Level.INFO, buildString(vals));
        }
    }

    public static void w(String tag, Object... vals) {
        if (isEnabled(tag)) {
            Logger.getLogger(tag).log(Level.WARNING, buildString(vals));
        }
    }

    public static void d(String tag, Object... vals) {
        if (isEnabled(tag)) {
            Logger.getLogger(tag).log(Level.FINE, buildString(vals));
        }
    }

    public static void e(String tag, Object... vals) {
        if (isEnabled(tag)) {
            Logger.getLogger(tag).log(Level.SEVERE, buildString(vals));
        }
    }

    public static void e(String tag, String log, Throwable t) {
        if (isEnabled(tag)) {
            Logger logger = Logger.getLogger(tag);
            logger.log(Level.SEVERE, log);
            logger.log(Level.SEVERE, t.toString());
            StackTraceElement[] eles = t.getStackTrace();
            for (StackTraceElement ele : eles) {
                logger.log(Level.SEVERE, ele.toString());
            }
        }
    }

    private static String buildString(Object... strings) {
        final StringBuilder sb = new StringBuilder();
        for (Object s : strings) {
            sb.append(s);
        }
        return sb.toString();
    }

}
