package com.kwizzad.property;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

/**
 * A {@link BehaviorSubject}, that allows reading out the current(last) value that was set to it.
 * You may initialize the Property with a value.
 *
 * @param <T> type
 */
public class Property<T> implements IProperty<T> {

    /**
     * the current value
     */
    protected volatile T value;

    /**
     * subject to observe the value
     */
    private BehaviorSubject<T> subject;

    /**
     * Create a new Property with an initial value of <i>null</i>
     */
    public Property() {
    }

    /**
     * Create a new property with an initial value
     *
     * @param initialValue the initial value of the property
     */
    public Property(T initialValue) {
        this.value = initialValue;
    }

    /**
     * Create a new Property with an initial value of <i>null</i>
     *
     * @param <T> type
     * @return property
     */
    public static <T> Property<T> create() {
        return new Property<>();
    }

    /**
     * Create a new property with an initial value
     *
     * @param initialValue the initial value of the property
     * @param <T>          type
     * @return property
     */
    public static <T> Property<T> create(T initialValue) {
        return new Property<>(initialValue);
    }

    /**
     * get the current value
     *
     * @return the current value
     */
    @Override
    public T get() {
        synchronized (this) {
            return value;
        }
    }

    /**
     * observe this property
     *
     * @return Observable of BehaviorSubject
     */
    @Override
    public Observable<T> observe() {
        synchronized (this) {
            if (subject == null) {
                subject = BehaviorSubject.createDefault(value);
            }
            return subject;
        }
    }

    /**
     * set the current value
     *
     * @param value new value
     */
    @Override
    public void set(T value) {
        synchronized (this) {
            this.value = value;
            notifyChanged();
        }
    }

    /**
     * notify all observers of the change
     */
    protected void notifyChanged() {
        synchronized (this) {
            if (subject != null) {
                subject.onNext(value);
            }
        }
    }
}
