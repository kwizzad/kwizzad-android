package com.kwizzad.model;

import com.kwizzad.db.PersistedProperty;
import com.kwizzad.property.Property;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Model {
    public volatile String advertisingId;
    public Property<Boolean> initialized = Property.create(false);

    private Map<String, PlacementModel> placements = new HashMap<>();

    public Property<Set<OpenTransaction>> openTransactions = Property.create(new HashSet<>());

    public final Property<String> apiKey = new PersistedProperty<String>("apiKey", String.class);
    public final Property<String> installId = new PersistedProperty<String>("installId", String.class);
    public final UserDataModel userDataModel = new UserDataModel();

    public String overrideWeb;
    public String overriddenBaseUrl;

    public String getBaseUrl(String apiKey) {
        if (this.overriddenBaseUrl != null) return overriddenBaseUrl;
        return "https://" + apiKey.substring(0, 7) + ".api.kwizzad.com/api/sdk/";
    }

    public PlacementModel getPlacement(String placementId) {
        if (!placements.containsKey(placementId)) {
            placements.put(placementId, new PlacementModel());
        }
        return placements.get(placementId);
    }
}
