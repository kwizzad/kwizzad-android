package com.kwizzad.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public final class JsonUtil {

    public static final boolean hasPropertyWithPrefix(JSONObject o, String prefix) throws JSONException {
        if (o == null)
            return false;

        for (Iterator<String> it = o.keys(); it.hasNext(); ) {
            String key = it.next();
            if (key.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    public static final String optFilledString(JSONObject json, String key) {
        return optFilledString(json, key, null);
    }

    public static final String optFilledString(JSONObject json, String key, String defaultValue) {
        String value = optString(json, key, defaultValue);
        if (value != null && value.trim().length() > 0)
            return value;
        else
            return defaultValue;
    }

    public static final String optString(JSONObject json, String key) {
        return optString(json, key, null);
    }

    public static final String optString(JSONObject json, String key, String defaultValue) {
        // http://code.google.com/p/android/issues/detail?id=13830
        if (json.isNull(key))
            return defaultValue;
        else
            return json.optString(key, defaultValue);
    }
}
