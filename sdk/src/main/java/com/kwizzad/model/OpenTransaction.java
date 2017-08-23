package com.kwizzad.model;

import com.kwizzad.Util;
import com.kwizzad.model.events.Reward;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class OpenTransaction {
    public final String adId;
    public final String transactionId;
    public final Date conversionTimestamp;
    public final Reward reward;

    public State state = State.ACTIVE;

    public OpenTransaction(JSONObject vo) throws JSONException {
        adId = vo.getString("adId");
        transactionId = vo.getString("transactionId");

        if (vo.has("conversionTimestamp"))
            conversionTimestamp = Util.fromISO8601(vo.getString("conversionTimestamp"));
        else
            conversionTimestamp = null;

        reward = Reward.from(vo.getJSONObject("reward"));
    }

    public enum State {
        ACTIVE, SENDING, SENT, ERROR
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;

        OpenTransaction other = (OpenTransaction) o;
        return other.adId.equals(adId) && other.transactionId.equals(transactionId);
    }

    @Override
    public int hashCode() {
        return (adId+"_"+transactionId ).hashCode();
    }

    @Override
    public String toString() {
        return "PendingEvent (" + state + " " + adId + " " + transactionId + " " + conversionTimestamp + " " + reward + ")";
    }
}
