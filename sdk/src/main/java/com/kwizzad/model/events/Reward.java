package com.kwizzad.model.events;

import com.kwizzad.log.QLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Reward {
    public final int amount;
    public final int maxAmount;
    public final String currency;
    public final Type type;

    public Reward(JSONObject vo) {
        amount = vo.optInt("amount", 0);
        maxAmount = vo.optInt("maxAmount", 0);
        currency = vo.optString("currency");
        type = Type.from(vo.optString("type"));
    }

    public enum Type {
        CALLBACK("callback"),
        CALL2ACTIONSTARTED("call2ActionStarted"),
        GOALREACHED("goalReached"),
        UNKNOWN("");

        public final String key;

        Type(String key) {
            this.key = key;
        }

        public static final Type from(String key) {
            if (key != null) {
                for (Type type : Type.values()) {
                    if (type.key.equalsIgnoreCase(key)) {
                        return type;
                    }
                }
            }
            return UNKNOWN;
        }
    }

    public static Reward from(JSONObject vo) throws JSONException {
        if (vo != null) {
            return new Reward(vo);
        }
        return null;
    }

    public static List<Reward> fromArray(JSONArray rewards) {
        List<Reward> ret = new ArrayList<>();
        for (int ii = 0; ii < rewards.length(); ii++) {
            try {
                ret.add(Reward.from(rewards.getJSONObject(ii)));
            } catch (Exception e) {
                QLog.d(e);
            }
        }
        return ret;

    }

    @Override
    public String toString() {
        return "Reward (" + type + ": " + amount + "," + currency + ")";
    }
}
