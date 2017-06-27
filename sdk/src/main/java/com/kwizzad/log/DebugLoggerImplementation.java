package com.kwizzad.log;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.kwizzad.util.Strings;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebugLoggerImplementation implements ILoggerImplementation {

    private final int skipDepth;
    private int minimumLogLevel = Log.VERBOSE;

    String tag = "";

    public DebugLoggerImplementation(Context context, int skipDepth) {
        this.skipDepth = skipDepth;
        String packageName = "";
        try {
            packageName = context.getPackageName();

            final int flags = context.getPackageManager().getApplicationInfo(packageName, 0).flags;
            minimumLogLevel = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ? Log.VERBOSE : Log.INFO;
            tag = packageName.toUpperCase();
        } catch (Exception e) {
            try {
                Log.e(packageName, "Error configuring logger", e);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public final void log(int logLevel, Throwable t) {
        if (minimumLogLevel <= logLevel) {
            println(logLevel, Log.getStackTraceString(t));
        }
    }

    public final void log(int logLevel, Object s1, Object... args) {
        if (minimumLogLevel > logLevel)
            return;

        final String s = Strings.toString(s1);
        final String message = formatArgs(s, args);
        println(logLevel, message);
    }

    public final void log(int logLevel, Throwable throwable, Object s1, Object[] args) {
        if (minimumLogLevel > logLevel)
            return;

        final String s = Strings.toString(s1);
        final String message = formatArgs(s, args) + '\n' + Log.getStackTraceString(throwable);
        println(logLevel, message);
    }

    void println(int priority, String msg) {

        Log.println(priority, tag, String.format("%s: %s", getTag(), processMessage(msg)));
    }

    private static final int MAX_THREAD_LN = 50;

    final String processMessage(String msg) {

        String threadName = Thread.currentThread().getName();

        if (threadName.length() > MAX_THREAD_LN) {

            try {
                threadName = String.valueOf(threadName.hashCode());//String.valueOf(MurmurHash.hash(threadName.getBytes(), 1));
            } catch (Exception e) {
                // fallback shortening
                threadName = threadName.replaceAll("(.)(?=\\1)", "");

                String newString = reduce(threadName);

                if (newString.length() > 5 && newString.length() < (MAX_THREAD_LN + 10)) {
                    threadName = newString;
                } else {

                    threadName = threadName.replaceAll("[aeiouäöü]", "");
                    if (threadName.length() > MAX_THREAD_LN) {
                        threadName = threadName.substring(0, MAX_THREAD_LN);
                    }
                }
            }
        }

        return String.format("%s %s", threadName, msg);
    }

    private String reduce(String threadName) {
        Pattern pattern = Pattern.compile("([^\\w]+)([\\w]*)");
        Matcher matcher = pattern.matcher(threadName);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {

            sb.append(matcher.group(1));
            String g = matcher.group(2);
            if (g.length() > 0)
                sb.append(g.substring(0, 1));
        }

        return sb.toString();
    }

    final String getTag() {
        final StackTraceElement trace = Thread.currentThread().getStackTrace()[skipDepth];

        return String.format(Locale.getDefault(), "(%s:%d) %s", trace.getFileName(), trace.getLineNumber(), trace.getMethodName());
    }

    private String formatArgs(final String s, Object... args) {
        if (args != null && args.length == 0) {
            return s;
        } else {
            return String.format(s, args);
        }
    }

}
