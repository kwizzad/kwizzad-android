package com.kwizzad.property;

import rx.Subscription;

/**
 * Wraps a {@link Subscription} so that it will unsubscribe automatically also on RxBinder.
 * All subscriptions provided by {@link RxSubscriber} are wrapped this way.
 *
 * @see rx.Subscription
 */
public final class SubscriptionWrapper implements Subscription {

    private final Subscription subscription;

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
    public SubscriptionWrapper(Subscription subscription, Object tag) {
        this.subscription = subscription;
        this.tag = tag;
    }


    /**
     * unsubscribe and remove the subscription getFrom {@link RxSubscriber}
     */
    @Override
    public void unsubscribe() {
        RxSubscriber.unsubscribe(tag, subscription);
    }

    /**
     * find out if the subsciption is still active
     *
     * @return if it is still subscribed
     */
    @Override
    public boolean isUnsubscribed() {
        return subscription.isUnsubscribed();
    }
}
