package com.kwizzad.model.events;

import com.kwizzad.db.FromJson;
import com.kwizzad.log.QLog;
import com.kwizzad.model.OpenTransaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OpenTransactionsEvent extends AEvent {

    public List<OpenTransaction> transactionList;

    @FromJson
    @Override
    public void from(JSONObject o) throws JSONException {
        super.from(o);

        transactionList = new ArrayList<>();

        JSONArray arr = o.getJSONArray("transactions");
        for (int ii = 0; ii < arr.length(); ii++) {
            JSONObject transactionJson = arr.getJSONObject(ii);
            try {
                transactionList.add(new OpenTransaction(transactionJson));
            } catch (Exception e) {
                QLog.d("open transaction error " + e.getLocalizedMessage() + ": " + transactionJson);
            }
        }
    }

    @Override
    public String toString() {
        return "OpenTransactionsEvent " + transactionList.size();
    }
}
