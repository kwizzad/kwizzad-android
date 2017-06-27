package com.kwizzad.model;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

/*** Datum
 * Ort
 * Zeit Von
 * Zeit Bis
 * Zeitzone
 * Titel
 * Description
 * URL
 * Erinnerung in X Min vorher
 */
public class CalendarEntryVO {

    public Calendar from;
    public Calendar to;
    public String tile;
    public String description;
    public String url;
    public String location;
    public Calendar alarm;

    public CalendarEntryVO from(JSONObject o) throws JSONException {



        return this;
    }
}
