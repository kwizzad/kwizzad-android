package com.kwizzad.log;

import android.util.Log;

public class QLog {

    private static ILoggerImplementation logger = new ReleaseLoggerImplementation();

    public static void setInstance(ILoggerImplementation newLogger) {
        logger = newLogger;
    }

    public static void v(Throwable throwable) {
        logger.log(Log.VERBOSE, throwable);
    }

    public static void v(Object s1, Object... args) {
        logger.log(Log.VERBOSE, s1, args);
    }

    public static void v(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.VERBOSE, throwable, s1, args);
    }

    public static void d(Throwable throwable) {
        logger.log(Log.DEBUG, throwable);
    }

    public static void d(Object s1, Object... args) {
        logger.log(Log.DEBUG, s1, args);
    }

    public static void d(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.DEBUG, throwable, s1, args);
    }

    public static void i(Throwable throwable) {
        logger.log(Log.INFO, throwable);
    }

    public static void i(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.INFO, throwable, s1, args);
    }

    public static void i(Object s1, Object... args) {
        logger.log(Log.INFO, s1, args);
    }

    public static void w(Throwable throwable) {
        logger.log(Log.WARN, throwable);
    }

    public static void w(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.WARN, throwable, s1, args);
    }

    public static void w(Object s1, Object... args) {
        logger.log(Log.WARN, s1, args);
    }

    public static void e(Throwable throwable) {
        logger.log(Log.ERROR, throwable);
    }

    public static void e(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.ERROR, throwable, s1, args);
    }

    public static void e(Object s1, Object... args) {
        logger.log(Log.ERROR, s1, args);
    }
}
