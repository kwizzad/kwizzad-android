package com.kwizzad.model;

public enum EPlatformType {
    UNKNOWN(null),
    ANDROID("Android");

    public final String key;

    EPlatformType(String key) {
        this.key = key;
    }

    public static EPlatformType fromKey(String key) {
        if (key == null)
            return UNKNOWN;

        for (EPlatformType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return UNKNOWN;
    }

}
