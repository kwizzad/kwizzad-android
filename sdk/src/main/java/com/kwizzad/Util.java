package com.kwizzad;

import com.kwizzad.log.QLog;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class Util {

    private static final DateFormat iso8601DateFormat;
    private static final DateFormat iso8601DateFormatMS;

    static {
        iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601DateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        iso8601DateFormatMS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        iso8601DateFormatMS.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String nowISO8601() {
        return toISO8601(new Date());
    }

    public static String toISO8601(Date date) {
        return iso8601DateFormatMS.format(date);
    }

    public static Date fromISO8601(String date) {
        try {
            return iso8601DateFormat.parse(date);
        } catch (ParseException e) {
            try {
                iso8601DateFormatMS.parse(date);
            } catch (Exception ee) {
                QLog.e("couldnt parse date " + date + " : " + ee.getLocalizedMessage());
            }
        }
        return null;
    }
}
