package com.kwizzad.property;

import java.util.Collection;
import java.util.List;

import rx.functions.Func1;

public final class AddConvertOperation<E, LIST_TYPE> implements IListOperation<LIST_TYPE> {

    private final Collection<E> source;
    private final Func1<E, LIST_TYPE> convertFunction;

    public AddConvertOperation(Collection<E> source, Func1<E, LIST_TYPE> convertFunction) {
        this.source = source;
        this.convertFunction = convertFunction;
    }

    @Override
    public void apply(List<LIST_TYPE> target) {
        for (E o : source) {
            target.add(convertFunction.call(o));
        }
    }
}
