package com.kwizzad.property;

import rx.Observable;

public interface IReadableProperty<T> {
    /**
     * get the current value. can be null
     *
     * @return the current value
     */
    T get();

    /**
     * start observing this property. this will initially fire with the current value.
     * That includes <i>null</i>!
     *
     * @return Observable
     */
    Observable<T> observe();
}
