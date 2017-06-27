package com.kwizzad.property;

import java.util.List;

/**
 * Add a certain Item at a specific location in the List
 *
 * @param <LIST_TYPE>
 */
public final class AddAtOperation<LIST_TYPE> implements IListOperation<LIST_TYPE> {
    private final LIST_TYPE o;
    private final int location;

    public AddAtOperation(LIST_TYPE o, int location) {
        this.o = o;
        this.location = location;
    }

    @Override
    public void apply(List<LIST_TYPE> target) {
        target.add(location, o);
    }
}
