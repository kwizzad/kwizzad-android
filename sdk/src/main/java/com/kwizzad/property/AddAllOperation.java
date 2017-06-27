package com.kwizzad.property;

import java.util.Collection;
import java.util.List;

/**
 * Operation for Add all items of the source to the List
 *
 * @param <LIST_TYPE> type
 */
public final class AddAllOperation<LIST_TYPE> implements IListOperation<LIST_TYPE> {
    /**
     * The source collection to add
     */
    private final Collection<LIST_TYPE> source;

    public AddAllOperation(Collection<LIST_TYPE> source) {
        this.source = source;
    }

    @Override
    public void apply(List<LIST_TYPE> target) {
        target.addAll(source);
    }
}
