package com.kwizzad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import com.kwizzad.api.KwizzadApi;
import com.kwizzad.db.DB;
import com.kwizzad.log.QLog;
import com.kwizzad.model.AdState;
import com.kwizzad.model.IUserDataModel;
import com.kwizzad.model.Model;
import com.kwizzad.model.OpenTransaction;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.Reward;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Kwizzad {

    private static final int MIN_SUPPORTED_OS_VERSION = Build.VERSION_CODES.KITKAT;

    private static AKwizzadBase impl;

    private static Model model = new Model();

    public static void init(Configuration configuration) {

        DB.instance.init(configuration.context);

        ISchedulers schedulers = new AsyncSchedulers();

        //init kwizzad api
        KwizzadApi.getInstance().init(model);

        impl = new KwizzadImpl(model, schedulers, configuration);

        impl.start();

        model.setInitialized(true);
    }

    private Kwizzad(){
        //private constuctor to prevent creating instances of this class
    }

    public static void requestAd(String placementId) {

        if (android.os.Build.VERSION.SDK_INT < MIN_SUPPORTED_OS_VERSION){
            // Do something for lollipop and above versions
            QLog.e("you are using old android version. any kwizzad ad will not be shown. (minimum supported android version is 4.4)");
            model.getPlacement(placementId).notifyError("you are using old android version. any kwizzad ad will not be shown. (minimum supported android version is 4.4)");
            return;
        }

        if(!isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            model.getPlacement(placementId).notifyError("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            return;
        }

        impl.requestAd(placementId);
    }

    public static Boolean getPreloadAdsAutomatically() {
        return impl.getPreloadAdsAutomatically();
    }

    /**
     * this param is true by default which means that it requests ad again after getting states:
     * NOFILL - some time after we receive this state;
     * DISMISSED - immediately after receiving this state;
     * RECEIVED - several seconds before ad is expired.
     * false means - dont reload ads after we get NOFILL, DISMISSED, RECEIVED states
     *
     * @param preloadAdsAutomatically
     */
    public static void setPreloadAdsAutomatically(Boolean preloadAdsAutomatically) {
        impl.setPreloadAdsAutomatically(preloadAdsAutomatically);
    }


    public static boolean canShowAd(String placementId) {
        IPlacementModel placementModel = model.getPlacement(placementId);
        if (placementModel.getAdState() == AdState.AD_READY) {
            return true;
        }
        return false;
    }


    public static boolean isInitialized() {
        return model.isInitialized();
    }

    public static void prepare(String placementId, Activity activity) {
        if(!isInitialized()) {
            QLog.d("sdk has not completed initialization yet. please wait until it finishes.");
            model.getPlacement(placementId).notifyError("sdk has not completed initialization yet. please wait until it finishes.");
            return;
        }

        impl.prepare(placementId, activity);
    }

    public static void start(String placementId, ViewGroup frame, Map<String, Object> customParameters) {
        if(!isInitialized()) {
            QLog.d("sdk has not completed initialization yet. please wait until it finishes.");
            model.getPlacement(placementId).notifyError("sdk has not completed initialization yet. please wait until it finishes.");
            return;
        }

        impl.start(placementId, frame, customParameters);
    }

    public static void close(String placementId) {
        if(!isInitialized()) {
            QLog.d("sdk has not completed initialization yet. please wait until it finishes.");
            model.getPlacement(placementId).notifyError("sdk has not completed initialization yet. please wait until it finishes.");
            return;
        }

        impl.close(placementId);
    }

    @Deprecated
    /**
     * Use getPlacementModel().getRewards() instead
     */
    public static Iterable<Reward> getRewards(String placementId) {
        AdResponseEvent adResponse = model.getPlacement(placementId).getAdResponse();
        if (adResponse != null) {
            return adResponse.rewards();
        } else
            return Collections.EMPTY_LIST;
    }

    /**
     * Set callback to receive current pending transactions
     * @param callback callback to receive current pending transactions
     */
    public static void setPendingTransactionsCallback(PendingTransactionsCallback callback) {
        impl.setPendingTransactionsCallback(callback);
    }

    /**
     * Finish the transaction. If the request works, this should remove the transaction getFrom the list and mark it completed
     * @param transaction transaction that must be closed
     */
    public static void completeTransaction(OpenTransaction transaction) {
        if(!isInitialized()) {
            QLog.d("sdk has not completed initialization yet. please wait until it finishes.");
            return;
        }

        impl.completeTransaction(transaction);
    }

    /**
     * Finish the transactions. If the request works, this should remove the transactions getFrom the list and mark it completed
     * @param openTransactions collection of transactions that must be closed
     */
    public static void completeTransactions(Collection<OpenTransaction> openTransactions) {

        if(!isInitialized()) {
            QLog.d("sdk has not completed initialization yet. please wait until it finishes.");
            return;
        }

        for (OpenTransaction transaction :
                openTransactions) {
            impl.completeTransaction(transaction);
        }
    }

    public static void resume(Context context) {
        impl.resume(context);
    }

    public static AdViewBuilder createAdViewBuilder() {
        return new AdViewBuilder();
    }

    /**
     * please use getPlacementModel()
     * @param placementId
     * @return
     */
    @Deprecated
    public static IPlacementModel getPlacement(String placementId) {
        return model.getPlacement(placementId);
    }

    public static IPlacementModel getPlacementModel(String placementId) {
        return model.getPlacement(placementId);
    }

    public static IUserDataModel getUserData() {
        return model.userDataModel;
    }

    static Model getModel() {
        return model;
    }


    public static class AdViewBuilder {
        private HashMap<String, Object> customParameters = new HashMap<>();
        private String placementId;

        public AdViewBuilder setPlacementId(String placementId) {
            this.placementId = placementId;
            return this;
        }

        public AdViewBuilder setCustomParameter(String key, Object value) {
            this.customParameters.put(key, value);
            return this;
        }

        public AdViewBuilder setCustomParameters(Map<String, Object> customParameters) {
            this.customParameters.putAll(customParameters);
            return this;
        }

        public AdSupportDialogFragment supportDialogFragment() {
            AdSupportDialogFragment fragment = new AdSupportDialogFragment();
            Bundle args = new Bundle();
            args.putString("PLACEMENT_ID", placementId);
            args.putSerializable("CUSTOM_PARAMS", customParameters);
            fragment.setArguments(args);
            return fragment;
        }

        public AdDialogFragment dialogFragment() {
            AdDialogFragment fragment = new AdDialogFragment();
            Bundle args = new Bundle();
            args.putString("PLACEMENT_ID", placementId);
            args.putSerializable("CUSTOM_PARAMS", customParameters);
            fragment.setArguments(args);
            return fragment;
        }

        public Intent activityIntent(Context context) {
            Intent intent = new Intent(context, AdActivity.class);
            intent.putExtra("PLACEMENT_ID", placementId);
            intent.putExtra("CUSTOM_PARAMS", customParameters);
            return intent;
        }
    }
}
