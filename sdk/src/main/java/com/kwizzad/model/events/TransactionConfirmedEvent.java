package com.kwizzad.model.events;

import com.kwizzad.db.ToJson;

import org.json.JSONException;
import org.json.JSONObject;

public class TransactionConfirmedEvent extends AAdEvent {

    public String transactionId;

    @ToJson
    @Override
    public void to(JSONObject o) throws JSONException {
        super.to(o);
        o.put("transactionId", transactionId);
    }
}
