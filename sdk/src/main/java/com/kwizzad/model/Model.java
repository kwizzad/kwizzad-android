package com.kwizzad.model;

import com.kwizzad.PlacementModel;
import com.kwizzad.db.PersistedProperty;
import com.kwizzad.property.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Model {
    private static final int serverIndex = new Random(System.currentTimeMillis()).nextInt(3) + 1;

    public volatile String advertisingId;
    public Property<Boolean> initialized = Property.create(false);

    private Map<String, PlacementModel> placements = new HashMap<>();

    public final Property<String> apiKey = new PersistedProperty<String>("apiKey", String.class);
    public final Property<String> installId = new PersistedProperty<String>("installId", String.class);

    public final UserDataModel userDataModel = new UserDataModel();

    public String overrideWeb;
    public String overriddenBaseUrl;

    public String getBaseUrl(String apiKey) {
        if (this.overriddenBaseUrl != null) return overriddenBaseUrl;
        return "https://" + apiKey.substring(0, 7) + "-" + Integer.toString(serverIndex) +  ".api.kwizzad.com/api/sdk/";
    }

    public PlacementModel getPlacement(String placementId) {
        if (!placements.containsKey(placementId)) {
            placements.put(placementId, new PlacementModel());
        }
        return placements.get(placementId);
    }

    public boolean noAdIsShowing() {
        for (Map.Entry<String, PlacementModel> entry : placements.entrySet()) {
            if (entry.getValue().getAdState() == AdState.SHOWING_AD ||
                    entry.getValue().getAdState() == AdState.CALL2ACTION ||
                    entry.getValue().getAdState() == AdState.CALL2ACTIONCLICKED ||
                    entry.getValue().getAdState() == AdState.GOAL_REACHED) {
                return false;
            }
        }
        return true;
    }
}
