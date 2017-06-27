package com.kwizzad.model.events;

import org.json.JSONException;
import org.json.JSONObject;

public class AAdEvent extends AEvent {

    public String placementId;
    public String adId;

    @Override
    public void from(JSONObject o) throws JSONException {
        super.from(o);
        if (!o.isNull("placementId"))
            this.placementId = o.optString("placementId");
        if (!o.isNull("adId"))
            this.adId = o.optString("adId", null);
    }

    @Override
    public void to(JSONObject o) throws JSONException {
        super.to(o);
        if (placementId != null)
            o.put("placementId", placementId);

        if (adId != null)
            o.put("adId", adId);
    }
}
