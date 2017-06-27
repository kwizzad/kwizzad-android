package com.kwizzad.property;

import com.kwizzad.util.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * ListProperty is basically just a normal List that allows you to bind to it for changes.
 * For convenience there is a {@link #beginTransaction()} so you can chain together multiple calls
 * with just once the binding firing at the end of it.
 *
 * @param <LIST_TYPE> The List Item Type
 */
public class ListProperty<LIST_TYPE> implements List<LIST_TYPE> {

    protected BehaviorSubject<List<LIST_TYPE>> subject;

    public static <LIST_TYPE> ListProperty<LIST_TYPE> create() {
        return new ListProperty<>();
    }

    protected final List<LIST_TYPE> c;

    protected boolean bindingEnabled = true;
    protected boolean pendingChanges = false;

    public ListProperty(List<LIST_TYPE> wrap) {
        this.c = wrap;
    }

    public ListProperty() {
        this(new ArrayList<>());
    }

    @Override
    public void add(int location, LIST_TYPE object) {
        c.add(location, object);
        notifyChanged();
    }

    @Override
    public boolean add(LIST_TYPE object) {
        c.add(object);
        notifyChanged();
        return true;
    }

    @Override
    public boolean addAll(int location, Collection<? extends LIST_TYPE> collection) {
        boolean ret = c.addAll(location, collection);
        if (ret) {
            notifyChanged();
        }
        return ret;
    }

    public boolean replace(Collection<? extends LIST_TYPE> collection) {
        c.clear();
        c.addAll(collection);
        notifyChanged();
        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean addUnchecked(Object object) {
        return add((LIST_TYPE) object);
    }

    @Override
    public boolean addAll(Collection<? extends LIST_TYPE> collection) {
        boolean ret = c.addAll(collection);
        if (ret) {
            notifyChanged();
        }
        return ret;
    }

    @Override
    public void clear() {
        int oldSize = c.size();
        if (oldSize > 0) {
            c.clear();
            notifyChanged();
        }
    }

    @Override
    public boolean contains(Object object) {
        return c.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return c.containsAll(collection);
    }

    @Override
    public LIST_TYPE get(int location) {
        return c.get(location);
    }

    @Override
    public int indexOf(Object object) {
        return c.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        return c.isEmpty();
    }

    @Override
    public Iterator<LIST_TYPE> iterator() {
        return c.iterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        return c.lastIndexOf(object);
    }

    @Override
    // TODO: wrap this to have binding
    public ListIterator<LIST_TYPE> listIterator() {
        return c.listIterator();
    }

    @Override
    // TODO: wrap this to have binding
    public ListIterator<LIST_TYPE> listIterator(int location) {
        return c.listIterator(location);
    }

    @Override
    public LIST_TYPE remove(int location) {
        LIST_TYPE ret = c.remove(location);
        if (ret != null) {
            notifyChanged();
        }
        return ret;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean remove(Object object) {
        boolean ret = c.remove(object);
        if (ret) {
            notifyChanged();
        }
        return ret;
    }

    @Override
    // TODO: implement this here to have binding
    public boolean removeAll(Collection<?> collection) {
        boolean ret = c.removeAll(collection);
        if (ret) {
            notifyChanged();
        }
        return ret;
    }

    @Override
    // TODO: implement this here to have binding
    public boolean retainAll(Collection<?> collection) {
        boolean ret = c.retainAll(collection);
        if (ret) {
            notifyChanged();
        }
        return ret;
    }

    @Override
    public LIST_TYPE set(int location, LIST_TYPE object) {
        LIST_TYPE ret = c.set(location, object);
        notifyChanged();
        return ret;
    }

    @Override
    public int size() {
        return c.size();
    }

    @Override
    // TODO: wrap this to have binding operations
    public List<LIST_TYPE> subList(int start, int end) {
        return c.subList(start, end);
    }

    @Override
    public Object[] toArray() {
        return c.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    public <T1> T1[] toArray(T1[] array) {
        return c.toArray(array);
    }

    public Observable<List<LIST_TYPE>> observe() {
        if (subject == null) {
            subject = BehaviorSubject.create(this);
        }
        return subject.asObservable();
    }

    public void notifyChanged() {
        if (bindingEnabled) {
            if (subject != null) {
                subject.onNext(this);
            }
        } else {
            pendingChanges = true;
        }
    }

    public void toggle(LIST_TYPE model) {
        if (c.contains(model)) {
            remove(model);
        } else {
            add(model);
        }
    }

    void setBindingEnabled(boolean value) {
        this.bindingEnabled = value;

        if (bindingEnabled && pendingChanges) {
            notifyChanged();
        }

        this.pendingChanges = false;
    }

    public ListTransaction<LIST_TYPE> beginTransaction() {
        return new ListTransaction<>(this);
    }

    @Override
    public String toString() {
        return "[ " + Strings.join(", ", this) + " ]";
    }
}
