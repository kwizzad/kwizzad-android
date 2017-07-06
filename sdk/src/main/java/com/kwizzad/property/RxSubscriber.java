package com.kwizzad.property;

import com.kwizzad.log.QLog;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Global Singleton Binder helper class, that allows you to subscribe/unsubscribe based on tags.
 * The tag can be any object, you want, so grouping does not pose a problem. What makes the most sense is up to you.
 *
 * @see io.reactivex.disposables.Disposable
 * @see io.reactivex.Observable#subscribe()
 */
public final class RxSubscriber {

    private static final Map<Object, LSubscriber> subscriberMap = new HashMap<>();

    private RxSubscriber() {
    }

    public static <T> Disposable subscribe(Object tag, Observable<? extends T> source, Consumer<? super T> target) {
        return new SubscriptionWrapper(getSubscriber(tag).subscribe(source, target, QLog::e), tag);
    }

    public static <T> Disposable subscribe(Object tag, Observable<? extends T> source, Consumer<? super T> onNext, Consumer<Throwable> error) {
        return new SubscriptionWrapper(getSubscriber(tag).subscribe(source, onNext, error), tag);
    }

    public static <T> void subscribe(Object tag, Observable<? extends T> source, Observer<T> subscriber) {
        getSubscriber(tag).subscribe(source, subscriber);
    }

    private static LSubscriber getSubscriber(Object tag) {
        LSubscriber b = subscriberMap.get(tag);
        if (b == null) {
            b = new LSubscriber();
            subscriberMap.put(tag, b);
        }
        return b;
    }

    public static void unsubscribe(Object tag, Disposable subscription) {
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
