package com.kwizzad.property;

import io.reactivex.disposables.Disposable;

/**
 * Wraps a {@link Disposable} so that it will unsubscribe automatically also on RxBinder.
 * All subscriptions provided by {@link RxSubscriber} are wrapped this way.
 *
 * @see io.reactivex.disposables.Disposable
 */
public final class SubscriptionWrapper implements Disposable {

    private final Disposable subscription;

    /**
     * The tag on {@link RxSubscriber}
     */
    private final Object tag;

    /**
     * Create a new subscription wrapper for a tag
     *
     * @param subscription The subscription
     * @param tag          The tag on {@link RxSubscriber}
     */
    public SubscriptionWrapper(Disposable subscription, Object tag) {
        this.subscription = subscription;
        this.tag = tag;
    }


    /**
     * unsubscribe and remove the subscription getFrom {@link RxSubscriber}
     */
    @Override
    public void dispose() {
        RxSubscriber.unsubscribe(tag, subscription);
    }

    /**
     * find out if the subsciption is still active
     *
     * @return if it is still subscribed
     */
    @Override
    public boolean isDisposed() {
        return subscription.isDisposed();
    }
}
