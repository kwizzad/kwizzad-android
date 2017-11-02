package com.kwizzad;

import com.kwizzad.model.AdState;
import com.kwizzad.model.ImageInfo;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.Reward;

import java.util.List;

/**
 * Created by tvsmiles on 05.10.17.
 */

public interface IPlacementModel {


    Iterable<Reward> getRewards();
    AdResponseEvent getAdResponse();
    List<ImageInfo> getAdImageUrls();
    String getTeaser();
    String getHeadline();
    String getBrand();
    State getState();

    AdState getAdState();
    boolean hasGoalUrl();

    void setWillPresentAdCallback(PlacementSimpleCallback callback);
    void setWillDismissAdCallback(PlacementSimpleCallback callback);
    void setPlacementStateCallback(PlacementStateCallback callback);

    void setErrorCallback(KwizzadErrorCallback errorCallback);

    void notifyError(String message);

    void notifyError(Throwable error);



    interface PlacementSimpleCallback {
        void callback();
    }

    interface PlacementStateCallback {
        void callback(AdState state);
    }

    final class State {
        public final long changed;
        public final AdState adState;

        public State(AdState adState) {
            this.adState = adState;
            this.changed = System.currentTimeMillis();
        }
    }
}
