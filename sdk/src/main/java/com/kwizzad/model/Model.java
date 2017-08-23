package com.kwizzad.model;

import com.kwizzad.AbstractPlacementModel;
import com.kwizzad.PlacementModel;
import com.kwizzad.db.PersistedProperty;
import com.kwizzad.property.Property;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Model {
    private static final int serverIndex = new Random(System.currentTimeMillis()).nextInt(3) + 1;

    public volatile String advertisingId;
    public Property<Boolean> initialized = Property.create(false);

    private Map<String, AbstractPlacementModel> placements = new HashMap<>();

    public Property<Set<OpenTransaction>> openTransactions = Property.create(new HashSet<>());

    public final Property<String> apiKey = new PersistedProperty<String>("apiKey", String.class);
    public final Property<String> installId = new PersistedProperty<String>("installId", String.class);
    public final UserDataModel userDataModel = new UserDataModel();

    public String overrideWeb;
    public String overriddenBaseUrl;

    public String getBaseUrl(String apiKey) {
        if (this.overriddenBaseUrl != null) return overriddenBaseUrl;
        return "https://" + apiKey.substring(0, 7) + "-" + Integer.toString(serverIndex) +  ".api.kwizzad.com/api/sdk/";
    }

    public AbstractPlacementModel getPlacement(String placementId) {
        if (!placements.containsKey(placementId)) {
            placements.put(placementId, new PlacementModel());
        }
        return placements.get(placementId);
    }
}
