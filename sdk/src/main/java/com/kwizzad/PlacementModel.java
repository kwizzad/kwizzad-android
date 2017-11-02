package com.kwizzad;

import com.kwizzad.log.QLog;
import com.kwizzad.model.AdState;
import com.kwizzad.model.ImageInfo;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.Reward;
import com.kwizzad.property.IReadableProperty;
import com.kwizzad.property.Property;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class PlacementModel implements  IPlacementModel {

    private PublishSubject<String> _pageStarted = PublishSubject.create();
    private PublishSubject<String> _pageFinished = PublishSubject.create();
    private final Property<AdResponseEvent.CloseType> _closeType = Property.create(AdResponseEvent.CloseType.OVERALL);
    private final Property<State> state = Property.create(new State(AdState.INITIAL));
    private final Property<Boolean> closeButtonVisible = Property.create(false);

    private AdResponseEvent adresponse;
    private int currentStep = 0;
    private String goalUrl;

    private Date retryAfter;

    private final PublishSubject<Throwable> errors = PublishSubject.create();
    private Disposable errorSubscription;


    Date getRetryAfter() { return retryAfter; }
    void setRetryAfter(Date retryAfter) { this.retryAfter = retryAfter; }



    public PlacementModel() {
        Observable.combineLatest(
                _closeType.observe(),
                state.observe(),
                (closeType1, state1) -> {
                    switch (closeType1) {
                        case AFTER_CALL2ACTION:
                        case AFTER_CALL2ACTION_PLUS:
                            return state1 != null &&
                                    (state1.adState == AdState.CALL2ACTIONCLICKED
                                            || state1.adState == AdState.GOAL_REACHED);

                        case BEFORE_CALL2ACTION:
                            return state1 != null &&
                                    (state1.adState == AdState.CALL2ACTION
                                            || state1.adState == AdState.CALL2ACTIONCLICKED
                                            || state1.adState == AdState.GOAL_REACHED);
                        default: // OVERALL
                            return true;
                    }
                }
        ).subscribe(closeButtonVisible::set);
    }

    @Override
    public AdResponseEvent getAdResponse() {
        return adresponse;
    }

    void setAdresponse(AdResponseEvent adresponse) {
        this.adresponse = adresponse;
        goalUrl = null;
        if (adresponse != null)
            _closeType.set(adresponse.closeButtonVisibility);
    }

    public AdState getAdState() {
        return state.get().adState;
    }


    public Iterable<Reward> getRewards() {
        if (adresponse != null) {
            return adresponse.rewards();
        } else
            return Collections.EMPTY_LIST;
    }

    @Override
    public List<ImageInfo> getAdImageUrls() {
        if(adresponse != null) {
            return adresponse.getImageUrls();
        } else {
            return null;
        }
    }

    @Override
    public String getTeaser() {
        if(adresponse != null) {
            return adresponse.getTeaser();
        } else {
            return null;
        }
    }

    @Override
    public String getHeadline() {
        if(adresponse != null) {
            return adresponse.getHeadLine();
        } else {
            return null;
        }
    }

    @Override
    public String getBrand() {
        if(adresponse != null) {
            return adresponse.getBrand();
        } else {
            return null;
        }
    }

    @Override
    public State getState() {
        return state.get();
    }

    @Override
    public boolean hasGoalUrl() {
        return false;
    }

    public Reward getReward(Reward.Type rewardType) {
        for(Reward reward : getRewards()) {
            if(reward.type == rewardType)
                return reward;
        }
        return null;
    }

    private WeakReference<PlacementSimpleCallback> willPresentCallback;
    private WeakReference<PlacementSimpleCallback> willDismissCallback;
    private WeakReference<PlacementStateCallback> stateCallback;
    private WeakReference<KwizzadErrorCallback> errorCallbackRef;

    private void willPresentAd() {
        if(willPresentCallback != null && willPresentCallback.get() != null) {
            willPresentCallback.get().callback();
        }
    }

    private void willDismissAd() {
        if(willDismissCallback != null && willDismissCallback.get() != null) {
            willDismissCallback.get().callback();
        }
    }

    @Override
    public void setWillPresentAdCallback(PlacementSimpleCallback callback) {
        willPresentCallback = new WeakReference<>(callback);
    }

    @Override
    public void setWillDismissAdCallback(PlacementSimpleCallback callback) {
        willDismissCallback = new WeakReference<>(callback);
    }

    @Override
    public void setPlacementStateCallback(PlacementStateCallback callback) {
        stateCallback = new WeakReference<>(callback);
    }

    Observable<State> observeState() {
        return state.observe();
    }

    void pageFinished(String url) {
        _pageFinished.onNext(url);
    }

    void pageStarted(String url) {
        _pageStarted.onNext(url);
    }

    void setAdState(AdState state) {
        QLog.d("changing state to "+state);

        this.state.set(new State(state));

        handleStateChangesCallbacks(state);
    }


    private void handleStateChangesCallbacks(AdState state) {
        if(stateCallback != null && stateCallback.get() != null) {
            stateCallback.get().callback(state);
        }
        switch (state) {
            case SHOWING_AD:
                willPresentAd();
                break;
            case DISMISSED:
                willDismissAd();
                break;
        }
    }


    String getGoalUrl() {
        return goalUrl;
    }

    void setGoalUrl(String goalUrl) {
        this.goalUrl = goalUrl;
    }

    void setCurrentStep(int i) {
        currentStep = i;
    }

    Object getCurrentStep() {
        return currentStep;
    }

    IReadableProperty<AdResponseEvent.CloseType> closeType() {
        return _closeType;
    }

    AdResponseEvent.CloseType getCloseType() {
        return closeType().get();
    }

    Observable<AdResponseEvent.CloseType> observeCloseType() {
        return closeType().observe();
    }

    boolean isCloseButtonVisible() {
        return closeButtonVisible.get();
    }

    Observable<Boolean> observeCloseButtonVisible() {
        return closeButtonVisible.observe();
    }

    Observable<String> pageStarted() {
        return _pageStarted;
    }

    Observable<String> pageFinished() {
        return _pageFinished;
    }


    @Override
    public void setErrorCallback(KwizzadErrorCallback errorCallback) {
        errorCallbackRef = new WeakReference<>(errorCallback);
        if(errorSubscription != null) {
            errorSubscription.dispose();
        }

        errorSubscription = errors.subscribeOn(AndroidSchedulers.mainThread()).subscribe(throwable -> {
            if(errorCallbackRef != null && errorCallbackRef.get() != null)
                errorCallbackRef.get().onError(throwable);
        });
    }

    @Override
    public void notifyError(String message) {
        setAdState(AdState.DISMISSED);
        errors.onNext(new Throwable(message));
    }

    @Override
    public void notifyError(Throwable error) {
        errors.onNext(error);
    }
}
