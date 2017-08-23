package com.kwizzad;

import com.kwizzad.model.AdState;
import com.kwizzad.model.ImageInfo;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.Reward;
import com.kwizzad.property.IReadableProperty;

import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public abstract class AbstractPlacementModel {

    private Date retryAfter;

    private final PublishSubject<Throwable> errors = PublishSubject.create();
    private Disposable errorSubscription;

    public abstract Iterable<Reward> getRewards();
    public abstract AdResponseEvent getAdResponse();
    public abstract List<ImageInfo> getAdImageUrls();
    public abstract String getTeaser();
    public abstract String getHeadline();
    public abstract String getBrand();
    public abstract State getState();
    public abstract AdState getAdState();
    public abstract boolean hasGoalUrl();

    public abstract void setWillPresentAdCallback(PlacementSimpleCallback callback);
    public abstract void setWillDismissAdCallback(PlacementSimpleCallback callback);
    public abstract void setPlacementStateCallback(PlacementStateCallback callback);


    @Deprecated
    abstract IReadableProperty<AdResponseEvent.CloseType> closeType();
    abstract Observable<Boolean> observeCloseButtonVisible();
    abstract Observable<String> pageStarted();
    abstract Observable<String> pageFinished();
    abstract void pageFinished(String url);
    abstract void pageStarted(String url);
    abstract AdResponseEvent.CloseType getCloseType();
    abstract Observable<AdResponseEvent.CloseType> observeCloseType();
    abstract Observable<State> observeState();
    abstract void setAdState(AdState state);
    abstract void setGoalUrl(String goalUrl);
    abstract void setCurrentStep(int i);
    abstract Object getCurrentStep();
    abstract String getGoalUrl();
    abstract void setAdresponse(AdResponseEvent adresponse);
    abstract boolean isCloseButtonVisible();


    Date getRetryAfter() { return retryAfter; }
    void setRetryAfter(Date retryAfter) { this.retryAfter = retryAfter; }


    public static final class State {
        public final long changed;
        public final AdState adState;

        public State(AdState adState) {
            this.adState = adState;
            this.changed = System.currentTimeMillis();
        }
    }

    public interface PlacementSimpleCallback {
        void callback();
    }

    public interface PlacementStateCallback {
        void callback(AdState state);
    }

    public void setErrorCallback(KwizzadErrorCallback errorCallback) {
        if(errorSubscription != null) {
            errorSubscription.dispose();
        }

        errorSubscription = errors.subscribe(throwable -> {
            setAdState(AdState.DISMISSED);
            errorCallback.onError(throwable);
        });
    }

    public void notifyError(String message) {
        errors.onNext(new Throwable(message));
    }

    public void notifyError(Throwable error) {
        errors.onNext(error);
    }
}
