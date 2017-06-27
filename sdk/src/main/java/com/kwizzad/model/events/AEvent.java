package com.kwizzad.model.events;

import com.kwizzad.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public abstract class AEvent {
    public final EEventType type;
    public Date eventTimestamp;

    public AEvent() {
        this.type = EventLookup.get(getClass());
    }

    public void from(JSONObject o) throws JSONException {
        if (o.isNull("eventTimestamp") == false)
            eventTimestamp = Util.fromISO8601(o.getString("eventTimestamp"));
    }

    public void to(JSONObject o) throws JSONException {
        if (eventTimestamp != null)
            o.put("eventTimestamp", Util.toISO8601(eventTimestamp));
        o.put("type", EventLookup.get(getClass()).key);
    }
}
