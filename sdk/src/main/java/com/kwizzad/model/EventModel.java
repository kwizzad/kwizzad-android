package com.kwizzad.model;

import com.kwizzad.db.ToJson;
import com.kwizzad.model.events.EEventType;

import org.json.JSONException;
import org.json.JSONObject;

public class EventModel<T> extends AComponentModel {

    public EEventType type;
    public Object event;

    public static EventModel from(JSONObject o) throws JSONException {
        EventModel m = new EventModel();
        m.type = EEventType.fromKey(o.getString("type"));
        return m;
    }

    @ToJson
    public void to(JSONObject o) {

    }
}
