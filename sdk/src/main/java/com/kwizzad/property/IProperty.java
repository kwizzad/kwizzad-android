package com.kwizzad.property;

public interface IProperty<T> extends IReadableProperty<T> {

    /**
     * Set a new value
     *
     * @param value new value
     */
    void set(T value);

}
