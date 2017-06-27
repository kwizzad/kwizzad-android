package com.kwizzad.property;

import java.util.List;

/**
 * Sets an item at a specific location
 *
 * @param <LIST_TYPE>
 */
public final class SetOperation<LIST_TYPE> implements IListOperation<LIST_TYPE> {
    private final LIST_TYPE o;
    private final int location;

    public SetOperation(LIST_TYPE o, int location) {
        this.o = o;
        this.location = location;
    }

    @Override
    public void apply(List<LIST_TYPE> target) {
        target.set(location, o);
    }
}
