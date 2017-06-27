package com.kwizzad.log;

import android.util.Log;

import com.kwizzad.util.Strings;

public class ReleaseLoggerImplementation implements ILoggerImplementation {

    protected final int minimumLogLevel;

    public ReleaseLoggerImplementation() {
        this(Log.INFO);
    }

    public ReleaseLoggerImplementation(int minimumLogLevel) {
        this.minimumLogLevel = minimumLogLevel;
    }

    public final void log(int logLevel, Throwable throwable) {
        if (minimumLogLevel <= Log.DEBUG)
            println(logLevel, Log.getStackTraceString(throwable));
    }

    public final void log(int logLevel, Throwable throwable, Object s1, Object... args) {
        if (minimumLogLevel > Log.DEBUG)
            return;
        final String s = Strings.toString(s1);
        final String message = formatArgs(s, args) + '\n' + Log.getStackTraceString(throwable);
        println(logLevel, message);
    }

    public final void log(int logLevel, Object s1, Object... args) {
        if (minimumLogLevel > Log.DEBUG)
            return;

        final String s = Strings.toString(s1);
        final String message = formatArgs(s, args);
        println(logLevel, message);
    }

    public final void println(int priority, String msg) {
        Log.println(priority, "LOG", msg);
    }

    protected final String formatArgs(final String s, Object... args) {
        if (args != null && args.length == 0) {
            return s;
        } else {
            return String.format(s, args);
        }
    }

}
