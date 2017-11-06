package com.kwizzad.model;

import com.kwizzad.PlacementModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Model {
    private static final int serverIndex = new Random(System.currentTimeMillis()).nextInt(3) + 1;

    public volatile String advertisingId;
    private boolean initialized = false;

    private Map<String, PlacementModel> placements = new HashMap<>();

    private String apiKey = "apiKey";
    private String installId = "installId";

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

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getInstallId() {
        return installId;
    }

    public void setInstallId(String installId) {
        this.installId = installId;
    }
}
