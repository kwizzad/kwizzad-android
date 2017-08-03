package com.kwizzad.property;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * Marks a transaction that will save commands to apply at once with one binding firing later.
 *
 * @param <LIST_TYPE>
 */
public class ListTransaction<LIST_TYPE> {

    private final ListProperty<LIST_TYPE> list;
    protected final Queue<IListOperation<LIST_TYPE>> ops = new ConcurrentLinkedQueue<>();

    ListTransaction(ListProperty<LIST_TYPE> ts) {
        this.list = ts;
    }

    public ListTransaction<LIST_TYPE> clear() {
        ops.add(new ClearOperation<>());
        return this;
    }

    public ListTransaction<LIST_TYPE> add(LIST_TYPE o) {
        ops.add(new AddOperation<>(o));
        return this;
    }

    public ListTransaction<LIST_TYPE> addAll(Collection<LIST_TYPE> o) {
        ops.add(new AddAllOperation<>(o));
        return this;
    }

    public <E> ListTransaction<LIST_TYPE> addAll(Collection<E> source, Function<E, LIST_TYPE> convertFunction) {
        ops.add(new AddConvertOperation<>(source, convertFunction));
        return this;
    }

    // additional ones

    public ListTransaction<LIST_TYPE> truncate(int offsetStart) {
        ops.add(new TruncateOperation<>(offsetStart));
        return this;
    }

    public ListTransaction<LIST_TYPE> remove(int location) {
        ops.add(new RemoveOperation<>(location));
        return this;
    }

    public ListTransaction<LIST_TYPE> remove(LIST_TYPE item) {
        ops.add(new RemoveOperation<>(item));
        return this;
    }

    public ListTransaction<LIST_TYPE> add(int location, LIST_TYPE o) {
        ops.add(new AddAtOperation<>(o, location));
        return this;
    }

    public ListTransaction<LIST_TYPE> set(int location, LIST_TYPE o) {
        ops.add(new SetOperation<>(o, location));
        return this;
    }

    public void reset() {
        ops.clear();
    }

    public void commit() {
        if (ops.isEmpty()) {
            return;
        }
        list.setBindingEnabled(false);

        while (!ops.isEmpty()) {
            final IListOperation<LIST_TYPE> c = ops.poll();

            if (c != null)
                c.apply(list);
        }

        list.setBindingEnabled(true);

    }


}
