package com.kwizzad.property;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public final class LSubscriber {

    final private Set<Disposable> disposables = new HashSet<>();

    public <T> Disposable subscribe(Observable<? extends T> source, Consumer<? super T> onNext) {
        Disposable subscription = source.subscribe(onNext);
        disposables.add(subscription);
        return subscription;
    }

    public <T> Disposable subscribe(Observable<? extends T> source, Consumer<? super T> onNext, Consumer<Throwable> error) {
        Disposable subscription = source.subscribe(onNext, error);
        disposables.add(subscription);
        return subscription;
    }

    public <T> void subscribe(Observable<? extends T> source, Observer<T> subscriber) {
        source.subscribe(subscriber);
    }

    public <T> Disposable subscribe(Single<? extends T> source, Consumer<? super T> onNext) {
        Disposable subscription = source.subscribe(onNext);
        disposables.add(subscription);
        return subscription;
    }

    public <T> Disposable subscribe(Single<? extends T> source, Consumer<? super T> onNext, Consumer<Throwable> error) {
        Disposable subscription = source.subscribe(onNext, error);
        disposables.add(subscription);
        return subscription;
    }

    public void unsubscribe(Disposable subscription) {
        if (disposables.contains(subscription)) {
            subscription.dispose();
            disposables.remove(subscription);
        }
    }

    public void unsubscribe() {
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
    }

    public boolean isEmpty() {
        return disposables.isEmpty();
    }

}
