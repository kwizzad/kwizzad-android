package com.kwizzad.property;

import com.kwizzad.log.QLog;

import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Global Singleton Binder helper class, that allows you to subscribe/unsubscribe based on tags.
 * The tag can be any object, you want, so grouping does not pose a problem. What makes the most sense is up to you.
 *
 * @see rx.Subscription
 * @see rx.Observable#subscribe()
 */
public final class RxSubscriber {

    private static final Map<Object, LSubscriber> subscriberMap = new HashMap<>();

    private RxSubscriber() {
    }

    public static <T> Subscription subscribe(Object tag, Observable<? extends T> source, Action1<? super T> target) {
        return new SubscriptionWrapper(getSubscriber(tag).subscribe(source, target, QLog::e), tag);
    }

    public static <T> Subscription subscribe(Object tag, Observable<? extends T> source, Action1<? super T> onNext, Action1<Throwable> error) {
        return new SubscriptionWrapper(getSubscriber(tag).subscribe(source, onNext, error), tag);
    }

    public static <T> Subscription subscribe(Object tag, Observable<? extends T> source, Subscriber<T> subscriber) {
        return new SubscriptionWrapper(getSubscriber(tag).subscribe(source, subscriber), tag);
    }

    private static LSubscriber getSubscriber(Object tag) {
        LSubscriber b = subscriberMap.get(tag);
        if (b == null) {
            b = new LSubscriber();
            subscriberMap.put(tag, b);
        }
        return b;
    }

    public static void unsubscribe(Object tag, Subscription subscription) {
        LSubscriber b = getSubscriber(tag);
        b.unsubscribe(subscription);
        if (b.isEmpty()) {
            subscriberMap.remove(tag);
        }
    }

    public static void unsubscribe(Object tag) {
        getSubscriber(tag).unsubscribe();

        subscriberMap.remove(tag);
    }
}
