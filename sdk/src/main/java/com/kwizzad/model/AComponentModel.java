package com.kwizzad.model;

import java.util.HashMap;
import java.util.Map;

public abstract class AComponentModel {

    private final Map<Class<? extends IComponent>, IComponent> componentMap = new HashMap<>();

    public <T extends IComponent> T get(Class<T> c) {
        return (T) componentMap.get(c);
    }

    protected <T extends IComponent> T getOrCreate(Class<T> c) {
        if (componentMap.containsKey(c)) {
            return (T) componentMap.get(c);
        } else {
            try {
                T instance = c.newInstance();
                componentMap.put(c, instance);
                return instance;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
