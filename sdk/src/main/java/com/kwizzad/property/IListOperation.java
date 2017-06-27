package com.kwizzad.property;

import java.util.List;

/**
 * A List Operation is there for the {@link ListTransaction} so these commands can be saved for applying them later.
 *
 * @param <LIST_TYPE> The List Item Type
 */
public interface IListOperation<LIST_TYPE> {

    /**
     * execute the operation on a given target
     *
     * @param target
     */
    void apply(List<LIST_TYPE> target);
}
