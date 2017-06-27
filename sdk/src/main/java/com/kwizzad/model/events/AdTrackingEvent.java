package com.kwizzad.model.events;

import com.kwizzad.Util;
import com.kwizzad.db.ToJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class AdTrackingEvent {

    private String adId;
    private Date eventTimestamp;
    private String type;
    private Map<String, Object> internalParameters = new HashMap<>();
    private Map<String, Object> customParameters = new HashMap<>();

    public static AdTrackingEvent create(String action, String adId) {
        AdTrackingEvent event = new AdTrackingEvent();
        event.adId = adId;
        event.eventTimestamp = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        event.type = action;
        return event;
    }

    public AdTrackingEvent internalParameter(String key, Object value) {
        internalParameters.put(key, value);
        return this;
    }

    public AdTrackingEvent setCustomParameter(String key, Object value) {
        customParameters.put(key, value);
        return this;
    }

    public AdTrackingEvent setCustomParameters(Map<String, Object> params) {
        if (params != null) {
            customParameters.putAll(params);
        }
        return this;
    }

    @ToJson
    public void to(JSONObject o) throws JSONException {
        if (eventTimestamp != null)
            o.put("eventTimestamp", Util.toISO8601(eventTimestamp));

        if (adId != null)
            o.put("adId", adId);

        if (customParameters != null && customParameters.size() > 0) {
            JSONArray params = new JSONArray();
            for (Map.Entry<String, Object> entry : customParameters.entrySet()) {
                JSONObject p = new JSONObject();
                p.put("key", entry.getKey());
                p.put("value", entry.getValue());
                params.put(p);
            }
            o.put("customParameters", params);
        }

        if (internalParameters != null && internalParameters.size() > 0) {
            for (Map.Entry<String, Object> entry : internalParameters.entrySet()) {
                o.put(entry.getKey(), entry.getValue());
            }
        }

        o.put("type", type);
    }

    @Override
    public String toString() {
        return "AdTrackingEvent " + type + ", " + adId + ", " + eventTimestamp;
    }
}
