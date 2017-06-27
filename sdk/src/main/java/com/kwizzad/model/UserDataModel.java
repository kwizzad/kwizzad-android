package com.kwizzad.model;

import android.location.Location;

import com.kwizzad.db.DB;
import com.kwizzad.db.ToJson;
import com.kwizzad.log.QLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UserDataModel implements IUserDataModel {

    public final Map<String, Object> data = new HashMap<>();

    UserDataModel(UserDataModel from) {
        data.putAll(from.data);
    }

    UserDataModel() {
        DB.instance.requestInit("userData", String.class, null, dbValue -> {
            QLog.d("userData initialized " + dbValue);

            if (dbValue != null && dbValue.trim().length() > 0) {
                try {
                    JSONObject o = new JSONObject(dbValue);

                    data.clear();

                    Iterator<String> it = o.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        data.put(key, o.get(key));
                    }

                } catch (Exception e) {
                    QLog.e(e);
                }
            }

        }, null);
    }

    private void set(String key, String value) {
        save(key, value);
    }

    private void set(String key, int value) {
        save(key, value);
    }

    private void set(String key, long value) {
        save(key, value);
    }

    private void set(String key, float value) {
        save(key, value);
    }

    private void set(String key, double value) {
        save(key, value);
    }

    private Object get(String key) {
        return data.get(key);
    }

    private String getString(String key) {
        Object o = get(key);
        if (o == null) {
            return null;
        }
        return String.valueOf(o);
    }

    private void save(String key, Object value) {
        if(value != null && data.get(key) != null && !value.equals(data.get(key))) {
            // unchanged
            return;
        }
        data.put(key, value);
        try {
            JSONObject o = new JSONObject();
            toJson(o);
//            QLog.d("storing userData "+o.toString());
            DB.instance.store("userData", o.toString(), null);
        } catch (JSONException e) {
            QLog.e(e);
        }
    }

    @ToJson
    public void toJson(JSONObject o) throws JSONException {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            o.put(entry.getKey(), entry.getValue());
        }
    }

    public UserDataModel realClone() {
        return new UserDataModel(this);
    }


    @Override
    public void setUserId(String userId) {
        set("userId", userId);
    }

    @Override
    public String getUserId() {
        return getString("userId");
    }

    @Override
    public void setGender(Gender gender) {
        set("gender", gender.name());
    }

    @Override
    public Gender getGender() {
        try {
            return Gender.valueOf(getString("gender"));
        } catch (Exception ignored) {
        }
        return Gender.UNKNOWN;
    }

    @Override
    public void setFacebookUserId(String id) {
        set("facebookUserId", id);
    }

    @Override
    public String getFacebookUserId() {
        return getString("facebookUserId");
    }

    @Override
    public void setName(String name) {
        set("userName", name);
    }

    @Override
    public String getName() {
        return getString("userName");
    }
}
