package com.kwizzad.model.events;

public enum EAdType {
    UNKNOWN(""),
    FULLSCREEN("adFullscreen");

    public final String key;

    EAdType(String key) {
        this.key = key;
    }

    public static EAdType fromKey(String key) {
        if (key == null)
            return UNKNOWN;

        for (EAdType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
