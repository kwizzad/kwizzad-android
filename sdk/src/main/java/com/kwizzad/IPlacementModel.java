package com.kwizzad;

import com.kwizzad.model.AdState;
import com.kwizzad.model.PlacementModel;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.Reward;
import com.kwizzad.property.IReadableProperty;

import rx.Observable;

public interface IPlacementModel {

    @Deprecated
    IReadableProperty<AdResponseEvent.CloseType> closeType();

    AdResponseEvent.CloseType getCloseType();
    Observable<AdResponseEvent.CloseType> observeCloseType();

    Iterable<Reward> getRewards();

    PlacementModel.State getState();
    Observable<PlacementModel.State> observeState();

    AdState getAdState();
    Observable<AdState> observeAdState();

    boolean isCloseButtonVisible();
    Observable<Boolean> observeCloseButtonVisible();

    Observable<String> pageStarted();
    Observable<String> pageFinished();

    boolean hasGoalUrl();
}
