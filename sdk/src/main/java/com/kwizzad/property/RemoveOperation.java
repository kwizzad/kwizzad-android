package com.kwizzad.property;

import java.util.List;

/**
 * Remove an item at a specific location or a specific getFrom this list.
 *
 * @param <LIST_TYPE>
 */
public final class RemoveOperation<LIST_TYPE> implements IListOperation<LIST_TYPE> {

    private final int location;
    private final LIST_TYPE item;

    /**
     * This will remove just a specific location.
     *
     * @param location
     */
    public RemoveOperation(int location) {
        this.location = location;
        item = null;
    }

    /**
     * This will remove a specific item getFrom the list.
     *
     * @param item
     */
    public RemoveOperation(LIST_TYPE item) {
        this.item = item;
        location = -1;
    }

    @Override
    public void apply(List<LIST_TYPE> target) {
        if (item != null)
            target.remove(item);
        else
            target.remove(location);
    }
}
