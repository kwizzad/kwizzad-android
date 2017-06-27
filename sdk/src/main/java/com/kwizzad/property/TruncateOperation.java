package com.kwizzad.property;

import java.util.List;

/**
 * Truncate the list to a certain amount of elements.
 *
 * @param <LIST_TYPE>
 */
public final class TruncateOperation<LIST_TYPE> implements IListOperation<LIST_TYPE> {

    private final int offsetStart;

    public TruncateOperation(int offsetStart) {
        this.offsetStart = offsetStart;
    }

    @Override
    public void apply(List<LIST_TYPE> target) {
        while (target.size() > offsetStart)
            target.remove(target.size() - 1);
    }
}
