package com.kwizzad.model.events;

import com.kwizzad.Util;
import com.kwizzad.db.FromJson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class NoFillEvent extends AAdEvent {
    public Date retryAfter;

    @FromJson
    @Override
    public void from(JSONObject o) throws JSONException {
        super.from(o);
        retryAfter = Util.fromISO8601(o.getString("retryAfter"));
    }
}
