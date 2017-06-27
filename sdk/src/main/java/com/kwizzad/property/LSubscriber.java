package com.kwizzad.property;

import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.Single;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;

public final class LSubscriber {

    final private Set<Subscription> subscriptions = new HashSet<>();

    public <T> Subscription subscribe(Observable<? extends T> source, Action1<? super T> onNext) {
        Subscription subscription = source.subscribe(onNext);
        subscriptions.add(subscription);
        return subscription;
    }

    public <T> Subscription subscribe(Observable<? extends T> source, Action1<? super T> onNext, Action1<Throwable> error) {
        Subscription subscription = source.subscribe(onNext, error);
        subscriptions.add(subscription);
        return subscription;
    }

    public <T> Subscription subscribe(Observable<? extends T> source, Subscriber<T> subscriber) {
        Subscription subscription = source.subscribe(subscriber);
        subscriptions.add(subscription);
        return subscription;
    }

    public <T> Subscription subscribe(Single<? extends T> source, Action1<? super T> onNext) {
        Subscription subscription = source.subscribe(onNext);
        subscriptions.add(subscription);
        return subscription;
    }

    public <T> Subscription subscribe(Single<? extends T> source, Action1<? super T> onNext, Action1<Throwable> error) {
        Subscription subscription = source.subscribe(onNext, error);
        subscriptions.add(subscription);
        return subscription;
    }

    public void unsubscribe(Subscription subscription) {
        if (subscriptions.contains(subscription)) {
            subscription.unsubscribe();
            subscriptions.remove(subscription);
        }
    }

    public void unsubscribe() {
        for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
        }
        subscriptions.clear();
    }

    public boolean isEmpty() {
        return subscriptions.isEmpty();
    }

}
