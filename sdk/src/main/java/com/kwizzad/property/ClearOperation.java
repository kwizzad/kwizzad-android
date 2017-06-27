package com.kwizzad.property;

import java.util.List;

public final class ClearOperation<LIST_TYPE> implements IListOperation<LIST_TYPE> {

    @Override
    public void apply(List<LIST_TYPE> target) {
        target.clear();
    }
}
