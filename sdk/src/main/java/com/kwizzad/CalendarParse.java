package com.kwizzad;

import android.content.Intent;
import android.provider.CalendarContract;

import com.kwizzad.log.QLog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 */
public class CalendarParse {

    public Intent createIntent(String ics) throws ParseException {

        /*Calendar beginTime = Calendar.getInstance();
        beginTime.set(2017, 0, 19, 7, 30);
        Calendar endTime = Calendar.getInstance();
        endTime.set(2017, 0, 19, 8, 30);*/

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI);

        ics = ics.substring(ics.indexOf("BEGIN:VEVENT") + "BEGIN:VEVENT".length() + 1, ics.indexOf("BEGIN:VALARM"));

        String[] lines = ics.split("\n");

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);

        for (String line : lines) {
            int si = line.indexOf(":");

            QLog.d("parse line: " + line + " " + si);

            String key = line.substring(0, si);
            String value = line.substring(si + 1, line.length());

            if (key.equals("LOCATION")) {
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, value);
            }

            if (key.equals("SUMMARY")) {
                intent.putExtra(CalendarContract.Events.TITLE, value);
            }

            if (key.equals("DESCRIPTION")) {
                intent.putExtra(CalendarContract.Events.DESCRIPTION, value);
            }

            if (key.startsWith("DTSTART;TZID")) {
                String tz = key.split("=")[1].trim();

                df.setTimeZone(TimeZone.getTimeZone(tz));

                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, df.parse(value.trim()).getTime());
            }

            if (key.startsWith("DTEND;TZID")) {
                String tz = key.split("=")[1].trim();

                df.setTimeZone(TimeZone.getTimeZone(tz));

                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, df.parse(value.trim()).getTime());
            }

        }





        /*
                //.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, 1)
                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                .putExtra(Intent.EXTRA_EMAIL, "foo@bar.com")
                .putExtra(CalendarContract.Events.RRULE);*/

        return intent;
    }
}
