package com.kwizzad.log;

import android.util.Log;

public class Logger {

    private final ILoggerImplementation logger;

    public Logger(ILoggerImplementation logger) {
        this.logger = logger;
    }

    public void v(Throwable throwable) {
        logger.log(Log.VERBOSE, throwable);
    }

    public void v(Object s1, Object... args) {
        logger.log(Log.VERBOSE, s1, args);
    }

    public void v(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.VERBOSE, throwable, s1, args);
    }

    public void d(Throwable throwable) {
        logger.log(Log.DEBUG, throwable);
    }

    public void d(Object s1, Object... args) {
        logger.log(Log.DEBUG, s1, args);
    }

    public void d(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.DEBUG, throwable, s1, args);
    }

    public void i(Throwable throwable) {
        logger.log(Log.INFO, throwable);
    }

    public void i(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.INFO, throwable, s1, args);
    }

    public void i(Object s1, Object... args) {
        logger.log(Log.INFO, s1, args);
    }

    public void w(Throwable throwable) {
        logger.log(Log.WARN, throwable);
    }

    public void w(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.WARN, throwable, s1, args);
    }

    public void w(Object s1, Object... args) {
        logger.log(Log.WARN, s1, args);
    }

    public void e(Throwable throwable) {
        logger.log(Log.ERROR, throwable);
    }

    public void e(Throwable throwable, Object s1, Object... args) {
        logger.log(Log.ERROR, throwable, s1, args);
    }

    public void e(Object s1, Object... args) {
        logger.log(Log.ERROR, s1, args);
    }
}
