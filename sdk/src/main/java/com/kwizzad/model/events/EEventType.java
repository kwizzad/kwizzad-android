package com.kwizzad.model.events;

public enum EEventType {

    UNKNOWN(""),

    //
    // REQUEST MAINLY
    //

    AD_REQUEST("adRequest"),
    TRANSACTION_CONFIRMED("transactionConfirmed"),
    //
    // RESPONSE MAINLY
    //

    AD_RESPONSE("adResponse"),
    NO_FILL("adNoFill"),
    OPEN_TRANSACTIONS("openTransactions");

    public final String key;

    EEventType(String key) {
        this.key = key;
    }

    public static EEventType fromKey(String key) {
        if (key == null)
            return UNKNOWN;

        for (EEventType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
