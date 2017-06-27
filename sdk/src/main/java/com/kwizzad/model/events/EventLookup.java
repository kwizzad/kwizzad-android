package com.kwizzad.model.events;

import java.util.HashMap;
import java.util.Map;

public class EventLookup {

    private static final Map<EEventType, Class<? extends AEvent>> map1 = new HashMap<>();
    private static final Map<Class<? extends AEvent>, EEventType> map2 = new HashMap<>();

    private static void add(EEventType type, Class<? extends AEvent> eventClass) {
        map1.put(type, eventClass);
        map2.put(eventClass, type);
    }

    static {
        add(EEventType.AD_REQUEST, AdRequestEvent.class);
        add(EEventType.AD_RESPONSE, AdResponseEvent.class);
        add(EEventType.OPEN_TRANSACTIONS, OpenTransactionsEvent.class);
        add(EEventType.NO_FILL, NoFillEvent.class);
        add(EEventType.TRANSACTION_CONFIRMED, TransactionConfirmedEvent.class);
    }

    public static EEventType get(Class<? extends AEvent> clazz) {
        return map2.get(clazz);
    }

    public static Class<? extends AEvent> get(String typeKey) {
        return get(EEventType.fromKey(typeKey));
    }

    public static Class<? extends AEvent> get(EEventType type) {
        if (type == null)
            return null;
        return map1.get(type);
    }
}
