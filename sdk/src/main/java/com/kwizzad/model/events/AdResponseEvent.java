package com.kwizzad.model.events;

import com.kwizzad.db.FromJson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdResponseEvent extends AAdEvent {

    public EAdType adType;
    public String expiry;
    public String url;
    public String goalUrlPattern;
    private List<Reward> rewardList = new ArrayList<>();

    public CloseType closeButtonVisibility;

    public enum CloseType {
        OVERALL("OVERALL"),
        BEFORE_CALL2ACTION("BEFORE_CALL2ACTION"),
        AFTER_CALL2ACTION("AFTER_CALL2ACTION"),
        AFTER_CALL2ACTION_PLUS("AFTER_CALL2ACTION_PLUS");

        public final String key;

        CloseType(String key) {
            this.key = key;
        }

        public static CloseType fromKey(String key) {
            if (key == null)
                return OVERALL;

            for (CloseType type : values()) {
                if (type.key.equalsIgnoreCase(key)) {
                    return type;
                }
            }
            return OVERALL;
        }
    }

    @FromJson
    @Override
    public void from(JSONObject o) throws JSONException {
        super.from(o);
        adType = EAdType.fromKey(o.getString("adType"));
        expiry = o.getString("expiry");
        url = o.getString("url");
        closeButtonVisibility = CloseType.fromKey(o.optString("closeButtonVisibility"));

        goalUrlPattern = o.optString("goalUrlPattern", null);
        if (goalUrlPattern != null && goalUrlPattern.trim().length() == 0)
            goalUrlPattern = null;

        rewardList.clear();
        if (o.has("rewards")) {
            rewardList = Reward.fromArray(o.getJSONArray("rewards"));
        }
    }

    public Iterable<Reward> rewards() {
        return rewardList;
    }
}
