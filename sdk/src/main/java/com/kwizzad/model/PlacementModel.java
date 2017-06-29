package com.kwizzad.model;

import com.kwizzad.IPlacementModel;
import com.kwizzad.log.QLog;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.Reward;
import com.kwizzad.property.IReadableProperty;
import com.kwizzad.property.Property;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.subjects.PublishSubject;

public class PlacementModel implements IPlacementModel {

    public int currentStep = 0;

    private PublishSubject<String> _pageStarted = PublishSubject.create();
    private PublishSubject<String> _pageFinished = PublishSubject.create();
    public String goalUrl;

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

    public AdResponseEvent getAdresponse() {
        return adresponse;
    }

    public void setAdresponse(AdResponseEvent adresponse) {
        this.adresponse = adresponse;
        goalUrl = null;
        if (adresponse != null)
            _closeType.set(adresponse.closeButtonVisibility);
    }

    @Override
    public IReadableProperty<AdResponseEvent.CloseType> closeType() {
        return _closeType;
    }

    @Override
    public AdResponseEvent.CloseType getCloseType() {
        return closeType().get();
    }

    @Override
    public Observable<AdResponseEvent.CloseType> observeCloseType() {
        return closeType().observe();
    }

    public static final class State {
        public final long changed;
        public final AdState adState;

        public State(AdState adState) {
            this.adState = adState;
            this.changed = System.currentTimeMillis();
        }
    }

    public final Property<State> state = Property.create(new State(AdState.INITIAL));
    private final Property<AdResponseEvent.CloseType> _closeType = Property.create(AdResponseEvent.CloseType.OVERALL);
    public final Property<Boolean> closeButtonVisible = Property.create(false);

    public AdState getAdState() {
        return state.get().adState;
    }

    public Observable<AdState> observeAdState() {
        return state.observe().map(pstate -> pstate.adState);
    }

    public void setAdState(AdState state) {
        QLog.d("changing state to "+state);

        handleStateChangesCallbacks(state);

        this.state.set(new State(state));
    }

    private void handleStateChangesCallbacks(AdState state) {
        if(stateCallback != null) {
            stateCallback.callback(state);
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

    private AdResponseEvent adresponse;
    public Date retryAfter;

    public Iterable<Reward> getRewards() {
        if (adresponse != null) {
            return adresponse.rewards();
        } else
            return Collections.EMPTY_LIST;
    }

    @Override
    public List<String> getAdImageUrls() {
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
    public Observable<State> observeState() {
        return state.observe();
    }

    @Override
    public boolean isCloseButtonVisible() {
        return closeButtonVisible.get();
    }

    @Override
    public Observable<Boolean> observeCloseButtonVisible() {
        return closeButtonVisible.observe();
    }

    @Override
    public Observable<String> pageStarted() {
        return _pageStarted;
    }

    public void pageStarted(String url) {
        _pageStarted.onNext(url);
    }

    @Override
    public Observable<String> pageFinished() {
        return _pageFinished;
    }

    @Override
    public boolean hasGoalUrl() {
        return false;
    }

    public void pageFinished(String url) {
        _pageFinished.onNext(url);
    }

    public Reward getReward(Reward.Type rewardType) {
        for(Reward reward : getRewards()) {
            if(reward.type == rewardType)
                return reward;
        }
        return null;
    }


    private PlacementSimpleCallback willPresentCallback;
    private PlacementSimpleCallback willDismissCallback;
    private PlacementStateCallback stateCallback;

    private void willPresentAd() {
        if(willPresentCallback != null) {
            willPresentCallback.callback();
        }
    }

    private void willDismissAd() {
        if(willDismissCallback != null) {
            willDismissCallback.callback();
        }
    }

    @Override
    public void setWillPresentAdCallback(PlacementSimpleCallback callback) {
        willPresentCallback = callback;
    }

    @Override
    public void setWillDismissAdCallback(PlacementSimpleCallback callback) {
        willDismissCallback = callback;
    }

    @Override
    public void setPlacementStateCallback(PlacementStateCallback callback) {
        stateCallback = callback;
    }
}
