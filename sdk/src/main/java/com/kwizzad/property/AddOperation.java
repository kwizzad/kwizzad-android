package com.kwizzad.property;

import java.util.List;

/**
 * Add an item to this list.
 *
 * @param <LIST_TYPE>
 */
public final class AddOperation<LIST_TYPE> implements IListOperation<LIST_TYPE> {
    private final LIST_TYPE o;

    public AddOperation(LIST_TYPE o) {
        this.o = o;
    }

    @Override
    public void apply(List<LIST_TYPE> target) {
        target.add(o);
    }
}
