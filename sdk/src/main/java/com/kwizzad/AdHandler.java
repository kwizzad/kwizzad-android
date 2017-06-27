package com.kwizzad;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kwizzad.log.QLog;
import com.kwizzad.model.AdState;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.Reward;
import com.kwizzad.property.RxSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

abstract class AdHandler {

    private String placementId;
    private HashMap<String, Object> customParameters = new HashMap<>();
    private boolean goalReached;

    private ProgressBar progress;

    private ImageView closeButton;
    private ViewGroup kwizzadView;

    private View closeDialog;
    private View closeDialogBackground;
    private TextView forfeitButton;
    private TextView claimButton;
    private TextView closeDialogTitle;
    private IPlacementModel placement;
    private Subscription lastTick;
    private boolean call2ActionClicked;

    public abstract void close();

    public abstract Activity activity();

    public void onCreate(Bundle parameters, Bundle savedInstanceState) {
        if (!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            close();
            return;
        }

        try {
            this.placementId = parameters.getString("PLACEMENT_ID");
            this.customParameters.clear();
            if (parameters.containsKey("CUSTOM_PARAMS")) {
                this.customParameters.putAll((Map<String, Object>) parameters.getSerializable("CUSTOM_PARAMS"));
            }
        } catch (Exception e) {
            this.placementId = savedInstanceState.getString("PLACEMENT_ID");
            this.customParameters.clear();
            this.customParameters.putAll((Map<String, Object>) savedInstanceState.getSerializable("CUSTOM_PARAMS"));
        }

        placement = Kwizzad.getPlacementModel(placementId);
    }

    private void showCloseDialog() {
        closeDialog.setVisibility(View.VISIBLE);


        Iterable<Reward> rewardList = Kwizzad.getPlacementModel(placementId).getRewards();
        Reward reward = null;

        for (Reward r : rewardList) {
            if (r.type == Reward.Type.CALLBACK) {
                reward = r;
            }
        }

        QLog.d("reward " + reward);

        if (call2ActionClicked && !(placement.hasGoalUrl())) {
            closeDialogTitle.setText(R.string.close_alert_title);
            forfeitButton.setText(R.string.close_alert_yes);
            claimButton.setText(R.string.close_alert_no);

        } else {
            if (reward != null && (reward.amount > 0 || reward.maxAmount > 0)) {

                String title = "";

                if (reward.maxAmount > 0) {
                    title = activity().getString(R.string.alert_close_title_var);
                    title = title.replace("#number_of_rewards#", String.valueOf(reward.maxAmount));
                } else {
                    title = activity().getString(R.string.alert_close_title);
                    title = title.replace("#number_of_rewards#", String.valueOf(reward.amount));
                }

                title = title.replace("#reward_name#", reward.currency);

                closeDialogTitle.setText(title);
            } else {

                closeDialogTitle.setText(R.string.alert_close_title_0);
            }
        }


    }

    public void onCreateView(View view) {
        if (!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            close();
            return;
        }

        closeButton = (ImageView) view.findViewById(R.id.kwizzadClose);

        if (closeButton != null) {
            closeButton.setOnClickListener((v) -> {
                if (goalReached)
                    close();
                else
                    showCloseDialog();
            });
        }

        progress = (ProgressBar) view.findViewById(R.id.progressBar);

        kwizzadView = (ViewGroup) view.findViewById(R.id.kwizzadView);
        closeDialog = view.findViewById(R.id.closeDialog);
        closeDialogBackground = view.findViewById(R.id.closeDialogBackground);
        closeDialogBackground.setOnClickListener((foo) -> closeDialog.setVisibility(View.GONE));
        forfeitButton = (TextView)view.findViewById(R.id.forfeitButton);
        forfeitButton.setOnClickListener(foo -> close());
        claimButton = (TextView)view.findViewById(R.id.claimButton);
        claimButton.setOnClickListener(foo -> closeDialog.setVisibility(View.GONE));
        closeDialogTitle = (TextView) view.findViewById(R.id.closeDialogTitle);
    }

    public void onDestroyView() {
        if (!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            close();
            return;
        }

        closeButton = null;
        progress = null;
        kwizzadView = null;
        closeDialog = null;
        closeDialogBackground.setOnClickListener(null);
        closeDialogBackground = null;
        forfeitButton.setOnClickListener(null);
        forfeitButton = null;
        claimButton.setOnClickListener(null);
        claimButton = null;
        closeDialogTitle = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        if (!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            close();
            return;
        }

        outState.putString("PLACEMENT_ID", placementId);
        outState.putSerializable("CUSTOM_PARAMS", customParameters);
    }

    public void onResume() {
        if (!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            close();
            return;
        }

        try {
            RxSubscriber.subscribe(this, placement.observeState(), placementState -> {
                try {
                    switch (placementState.adState) {
                        case LOADING_AD:
                        case INITIAL:
                        case REQUESTING_AD:
                            progress.setVisibility(View.VISIBLE);
                            break;
                        case RECEIVED_AD:
                            Kwizzad.prepare(placementId, activity());
                            progress.setVisibility(View.VISIBLE);
                            break;
                        case AD_READY:
                            progress.setVisibility(View.INVISIBLE);
                            Kwizzad.start(placementId, kwizzadView, customParameters);
                            break;
                        case GOAL_REACHED:
                            goalReached = true;
                            closeButton.setImageResource(R.drawable.okay_button);
                            break;
                        case CALL2ACTIONCLICKED:
                            call2ActionClicked = true;
                            break;
                        case DISMISSED:
                        case NOFILL:
                            try {
                                close();
                            } catch (Exception ignored) {
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            RxSubscriber.subscribe(this, placement.observeCloseButtonVisible(),
                    (closeButtonVisible) -> {
                        if (closeButton != null) {
                            closeButton.setVisibility(closeButtonVisible ? View.VISIBLE : View.GONE);
                        }
                    });

            RxSubscriber.subscribe(this, placement.pageStarted(), url -> {
                if (placement.getAdState() == AdState.SHOWING_AD
                        || placement.getAdState() == AdState.CALL2ACTION
                        || placement.getAdState() == AdState.CALL2ACTIONCLICKED
                        || placement.getAdState() == AdState.GOAL_REACHED) {

                    if (lastTick != null)
                        lastTick.unsubscribe();

                    progress.setVisibility(View.GONE);

                    QLog.d("page started " + url);


                    lastTick = Observable
                            .timer(500, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(foo -> {
                                if (progress != null)
                                    progress.setVisibility(View.VISIBLE);
                            });
                }
            });

            RxSubscriber.subscribe(this, placement.pageFinished(), url -> {

                QLog.d("page finished " + url);

                if (progress == null)
                    return;

                if (placement.getAdState() == AdState.SHOWING_AD
                        || placement.getAdState() == AdState.CALL2ACTION
                        || placement.getAdState() == AdState.CALL2ACTIONCLICKED
                        || placement.getAdState() == AdState.GOAL_REACHED) {

                    if (lastTick != null)
                        lastTick.unsubscribe();

                    progress.setVisibility(View.GONE);
                }

            });

        } catch (Exception e) {
            QLog.d(e);
            close();
        }
    }

    public void onBackPressed() {
        if (!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            close();
            return;
        }

        if (placement.isCloseButtonVisible()
                || placement.getCloseType() == AdResponseEvent.CloseType.AFTER_CALL2ACTION_PLUS
                || placement.getCloseType() == AdResponseEvent.CloseType.OVERALL) {
            if (goalReached) {
                close();
            } else {
                showCloseDialog();
            }
        }
    }

    public void onPause() {
        RxSubscriber.unsubscribe(this);
    }

    public void onDestroy() {
        if (!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            return;
        }

        try {
            Kwizzad.close(placementId);
        } catch (Exception e) {
            QLog.d(e);
        }
    }

}
