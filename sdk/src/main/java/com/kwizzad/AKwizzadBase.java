package com.kwizzad;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.kwizzad.api.HttpErrorResponseException;
import com.kwizzad.api.QueryParams;
import com.kwizzad.api.KwizzadApi;
import com.kwizzad.log.DebugLoggerImplementation;
import com.kwizzad.log.QLog;
import com.kwizzad.model.AdState;
import com.kwizzad.model.Model;
import com.kwizzad.model.OpenTransaction;
import com.kwizzad.model.events.AEvent;
import com.kwizzad.model.events.AdRequestEvent;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.AdTrackingEvent;
import com.kwizzad.model.events.NoFillEvent;
import com.kwizzad.model.events.OpenTransactionsEvent;
import com.kwizzad.model.events.TransactionConfirmedEvent;
import com.kwizzad.property.Property;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

public class AKwizzadBase {

    private static final long TIMEOUT_REQUESTING_AD = 1000 * 10;

    private final ISchedulers schedulers;
    private final Model model;

    private final Context applicationContext;
    private WebView currentWebView;
    private String currentPlacement;

    private Disposable lastSubscription;
    private Disposable stateSubscription;
    private Disposable transactionsSubscription;
    private Disposable rerequestSubscription;
    private WeakReference<PendingTransactionsCallback> transactionsCallback;

    private Property<List<Object>> scheduledTrackingEventsProperty = Property.create(new ArrayList<>());

    private Property<Set<OpenTransaction>> openTransactions = Property.create(new HashSet<>());

    public Boolean preloadAdsAutomatically = true;

    public AKwizzadBase(Model model, ISchedulers schedulers, Configuration configuration) {
        this.schedulers = schedulers;
        this.model = model;
        this.applicationContext = configuration.context;

        if (configuration.debug) {
            QLog.setInstance(new DebugLoggerImplementation(configuration.context, 6));
        }

        // TODO: check for change!
        model.setApiKey(configuration.apiKey);
        model.overriddenBaseUrl = configuration.overrideServer;
        model.overrideWeb = configuration.overrideWeb;

        if (model.getInstallId()== null) {
            model.setInstallId(UUID.randomUUID().toString());
        }
    }


    public void start() {
        KwizzadApi.getInstance().observe(OpenTransactionsEvent.class)
                .observeOn(schedulers.mainThread())
                .subscribe(openTransactionsEvent -> {
                    if (!model.noAdIsShowing()) return;

                    QLog.d("got open transactions " + openTransactionsEvent);

                    Set<OpenTransaction> newSet = new HashSet<>();
                    newSet.addAll(openTransactionsEvent.transactionList);
                    newSet.removeAll(openTransactions.get());


                    if (newSet.size() > 0) {
                        QLog.d("setting transactions");
                        openTransactions.set(newSet);
                    }

                });

        KwizzadApi.getInstance().observe(AdResponseEvent.class)
                .observeOn(schedulers.mainThread())
                .subscribe((event) -> {
                    final PlacementModel m = model.getPlacement(event.placementId);
                    if (m.getAdState() == AdState.REQUESTING_AD) {

                        if (BuildConfig.DEBUG && model.overrideWeb != null) {
                            QLog.d("Replace " + event.url + " with " + model.overrideWeb);
                            event.url = event.url.replaceFirst("[^:]+://[^/]+", model.overrideWeb);
                            QLog.d("Replaced: " + event.url);
                        }

                        m.setAdresponse(event);

                        m.setAdState(AdState.RECEIVED_AD);
                    }
                });

        KwizzadApi.getInstance().observe(NoFillEvent.class)
                .observeOn(schedulers.mainThread())
                .subscribe(event -> {
                    final PlacementModel m = model.getPlacement(event.placementId);
                    if (m.getAdState() == AdState.REQUESTING_AD) {
                        m.setAdresponse(null);
                        m.setRetryAfter(event.retryAfter);
                        m.setAdState(AdState.NOFILL);
                    }
                });

        final List<Object> eventsSending = new ArrayList<>();

        scheduledTrackingEventsProperty.observe()
                .observeOn(schedulers.mainThread())
                .filter(objects -> objects.size() > 0 && eventsSending.size() == 0)
                .flatMap(objects -> {
                    eventsSending.clear();
                    eventsSending.addAll(objects);
                    StringBuilder sb = new StringBuilder("sending events: \n");
                    for (Object event : eventsSending) {
                        sb.append(event.toString());
                        sb.append("\n");
                    }
                    QLog.d(sb.toString());
                    return Observable.just(new ArrayList<>(eventsSending));
                })
               .subscribe(objects -> {
                    Observable.fromIterable(objects)
                            .observeOn(schedulers.io())
                            .flatMap(KwizzadApi.getInstance()::send)
                            .observeOn(schedulers.mainThread())
                            // also main thread!
                            .retryWhen(errors -> errors.flatMap(error -> {

                                eventsSending.clear();

                                QLog.e("error sending events: " + error.getMessage());
                                return defaultErrorHandler(error);
                            }))
                            .subscribe(new Observer<AEvent>() {

                                @Override
                                public void onNext(AEvent aEvent) {}

                                @Override
                                public void onSubscribe(@NonNull Disposable d) {}

                                @Override
                                public void onError(Throwable error) {
                                    QLog.e(error, "error sending tracking events");
                                   // errors.onNext(error); dont handle errors for now, but need to find a way to handle them in placement model
                                }

                                @Override
                                public void onComplete() {
                                    if (objects.size() > 0) {
                                        List<Object> events = scheduledTrackingEventsProperty.get();
                                        events.removeAll(objects);
                                        eventsSending.removeAll(objects);
                                        scheduledTrackingEventsProperty.set(events);
                                        QLog.d("successfully sent " + eventsSending.size() + " tracking events");
                                    }
                                }
                            });
                });

    }
/*
503:Service Unavailable: Back-end server is at capacity
11-02 17:35:02.154 8682-8682/com.kwizzad.example D/COM.KWIZZAD.EXAMPLE: (AKwizzadBase.java:158) lambda$start$11: main sending events:
:(((
 */

    private Observable<Long> defaultErrorHandler(Throwable error) {
        if (error instanceof HttpErrorResponseException) {
            HttpErrorResponseException e = (HttpErrorResponseException) error;
            if (e.isServerError()) {
                return Observable.timer(120, TimeUnit.SECONDS);
            }
        }
        if (error instanceof IOException)
            return Observable.timer(60, TimeUnit.SECONDS);
        else {
            QLog.e(error);
            return Observable.error(error);
        }
    }



    public void setPendingTransactionsCallback(PendingTransactionsCallback callback) {
        transactionsCallback = new WeakReference<>(callback);
        if(transactionsSubscription != null) {
            transactionsSubscription.dispose();
            transactionsSubscription = null;
        }

        if(callback != null) {
            transactionsSubscription = pendingTransactions()
                    .subscribe(transactions -> {
                        if (transactionsCallback != null && transactionsCallback.get() != null) {
                            transactionsCallback.get().callback(transactions);
                        }
                    });
        }
    }

    public void requestAd(String placementId) {
        QLog.d("request ad for " + placementId);

        final PlacementModel placementModel = model.getPlacement(placementId);

        IPlacementModel.State placementState = placementModel.getState();
        switch (placementState.adState) {
            case REQUESTING_AD:
                // ignoring new request

                if ((placementState.changed + TIMEOUT_REQUESTING_AD) > System.currentTimeMillis()) {
                    QLog.e("trying to request a new ad while currently ad request running. will be ignored.");
                    return;
                } else {
                    QLog.i("placement was requesting ad, but timeout was reached");
                }
                break;
            case RECEIVED_AD:
                QLog.i("already have an ad - use it.");
                return;
            case NOFILL:
                Date now = new Date();
                if (placementModel.getRetryAfter() != null && placementModel.getRetryAfter().after(now)) {
                    QLog.i("no fill said to retry after " + placementModel.getRetryAfter() + " but we have " + now + ". ignoring request for now.");
                    return;
                }
                break;
        }
        // TODO: check other states

        //request add again after some delay
        if (stateSubscription != null) {
            stateSubscription.dispose();
        }
        if(preloadAdsAutomatically) {
            stateSubscription = placementModel.observeState()
                    .skip(1) // skip first because we dont want to get current state, only state changes
                    .subscribe(state -> {
                        switch (state.adState) {
                            case NOFILL:
                                requestAdAfterDelay(placementId, (placementModel.getRetryAfter().getTime() - System.currentTimeMillis()));
                                break;
                            case RECEIVED_AD:
                                requestAdAfterDelay(placementId, placementModel.getAdResponse().getExpiresIn());
                                break;
                            case DISMISSED:
                                if (model.getPlacement(placementId).getAdResponse() != null) {
                                    requestAdAfterDelay(placementId, 0);
                                }
                                break;
                            default:
                                break;
                        }
                    });
        }

        placementModel.setAdState(AdState.REQUESTING_AD);

        Observable
                .fromCallable(() -> {
                    QLog.d("checking idfa");

                    AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext);
                    if (info == null || info.isLimitAdTrackingEnabled()) {
                        QLog.d("no ad tracking enabled :(");
                        model.advertisingId = null;
                    } else {
                        model.advertisingId = info.getId();
                    }

                    AdRequestEvent requestEvent = new AdRequestEvent(applicationContext);
                    requestEvent.placementId = placementId;
                    requestEvent.idfa = model.advertisingId;
                    requestEvent.userDataModel = model.userDataModel.realClone();
                    return requestEvent;
                })
                .flatMap(requestEvent -> {
                    QLog.d("requesting the ad for " + placementId);
                    return KwizzadApi.getInstance().send(requestEvent);
                })
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.mainThread())
                .subscribe(new Observer<AEvent>() {

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {}

                    @Override
                    public void onError(Throwable error) {
                        QLog.e(error, "error requesting ad");

                        if (placementModel.getAdState() == AdState.REQUESTING_AD) {
                            placementModel.notifyError(error);
                            return;
                        }

                        placementModel.notifyError(error);
                        if (error instanceof IOException) {
                            return;
                        }
                        if (error instanceof GooglePlayServicesNotAvailableException) {
                            // we dont handle for now
                        }
                        if (error instanceof GooglePlayServicesRepairableException) {
                            // we dont handle for now
                        }

                    }

                    @Override
                    public void onNext(AEvent event) {
                        QLog.d("EVENT " + event.type);
                    }

                    @Override
                    public void onComplete() {}
                });
    }


    private void requestAdAfterDelay(String placementId, long delayInMilliseconds) {
        QLog.d("requesting again after "
                + delayInMilliseconds / 1000
                + " seconds");

        if(rerequestSubscription != null) {
            rerequestSubscription.dispose();
        }

        rerequestSubscription = Observable.just(placementId)
                .delay(delayInMilliseconds, TimeUnit.MILLISECONDS)
                .observeOn(schedulers.mainThread())
                .flatMap(placementIdObj -> {
                    requestAd(placementIdObj);
                    return Observable.just(true);
                })
                .publish()
                .connect();
    }


    public void prepare(String placementId, Activity activity) {
        final PlacementModel placementModel = model.getPlacement(placementId);

        if (activity == null || activity.getBaseContext() == null) {
            QLog.e("Application Lifecycle Error: Can only prepare ads if Activity and Context initialized!");
            return;
        }

        if (placementModel.getAdState() == AdState.RECEIVED_AD) {

            QLog.d("preparing " + placementId);

            placementModel.setCurrentStep(0);
            placementModel.setRetryAfter(null);

            clearWebView();

            if (currentWebView != null) {

                // if it was another placement, cancel the other placement
                if (!currentPlacement.equals(placementId)) {
                    // TODO: shouldnt happen, but could
                    model.getPlacement(currentPlacement).setAdState(AdState.DISMISSED);
                }
            }

            currentPlacement = placementId;

            currentWebView = new WebView(activity);
            currentWebView.setId(R.id.kwizzadWebView);

            Display display = activity.getWindowManager().getDefaultDisplay();
            int realWidth;
            int realHeight;

            if (Build.VERSION.SDK_INT >= 17) {
                //new pleasant way to get real metrics
                DisplayMetrics realMetrics = new DisplayMetrics();
                display.getRealMetrics(realMetrics);
                realWidth = realMetrics.widthPixels;
                realHeight = realMetrics.heightPixels;
            } else if (Build.VERSION.SDK_INT >= 14) {
                //reflection for this weird in-between time
                try {
                    Method mGetRawH = Display.class.getMethod("getRawHeight");
                    Method mGetRawW = Display.class.getMethod("getRawWidth");
                    realWidth = (Integer) mGetRawW.invoke(display);
                    realHeight = (Integer) mGetRawH.invoke(display);
                } catch (Exception e) {
                    //this may not be 100% accurate, but it's all we've got
                    realWidth = display.getWidth();
                    realHeight = display.getHeight();
                    Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
                }

            } else {
                //This should be close, as lower API devices should not have window navigation bars
                realWidth = display.getWidth();
                realHeight = display.getHeight();
            }

            currentWebView.layout(0, 0, realWidth, realHeight);

            currentWebView.getSettings().setJavaScriptEnabled(true);
            currentWebView.getSettings().setAppCacheEnabled(true);
            currentWebView.getSettings().setDomStorageEnabled(true);
            currentWebView.getSettings().setUseWideViewPort(true);
            currentWebView.getSettings().setLoadWithOverviewMode(true);
            currentWebView.getSettings().setAppCachePath(currentWebView.getContext().getCacheDir().getAbsolutePath());
            currentWebView.getSettings().setSupportMultipleWindows(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(currentWebView, true);
            }
            currentWebView.setBackgroundColor(0xffffffff);


            if (lastSubscription != null) {
                lastSubscription.dispose();
                lastSubscription = null;
            }
            lastSubscription = placementModel.observeState().subscribe(state -> {
                if (state.adState == AdState.DISMISSED) {
                    if (currentWebView != null) {

                        QLog.d("removing webview");

                        currentWebView.removeJavascriptInterface("KwizzAdJI");
                        ViewGroup parent = (ViewGroup) currentWebView.getParent();
                        if (parent != null) {
                            try {
                                parent.removeAllViews();
                                currentWebView.destroy();
                            } catch (Exception e) {
                                QLog.d(e, "ooooh");
                            }
                        }
                        currentWebView = null;
                    }
                    if (lastSubscription != null) {
                        lastSubscription.dispose();
                        lastSubscription = null;
                    }
                }
            });

            new PlacementJavascriptInterface(currentWebView, placementModel, this);

            placementModel.setAdState(AdState.LOADING_AD);

            String urlWithKometParams = QueryParams.addCometQueryParams(placementModel.getAdResponse().url, model, applicationContext);
            QLog.d("url : " + placementModel.getAdResponse().url + ", url with additional komet params : " + urlWithKometParams);

            currentWebView.loadUrl(placementModel.getAdResponse().url);
            final AdResponseEvent adresponse = placementModel.getAdResponse();
            new Handler().postDelayed(() -> {
                if (placementModel.getAdState() == AdState.LOADING_AD && adresponse == placementModel.getAdResponse()) {
                    QLog.w("ad loading timeout reached. this shouldnt happen, but it did. cancelling ad :(");
                    placementModel.setAdresponse(null);
                    placementModel.notifyError("ad loading timeout reached. this shouldnt happen, but it did. cancelling ad :(");
                }
            }, 22000);
        } else {
            QLog.e("can only prepare ads if in the state " + AdState.RECEIVED_AD + ", but placement was in state " + placementModel.getAdState());
        }
    }

    private void clearWebView() {
        if (currentWebView != null) {
            QLog.d("removing webview");
            currentWebView.clearHistory();
            currentWebView.clearCache(true);
            currentWebView = null;

            try {
                applicationContext.deleteDatabase("webview.db");
            } catch (Exception ignored) {
            }
            try {
                applicationContext.deleteDatabase("webviewCache.db");
            } catch (Exception ignored) {
            }
        }
    }

    public void start(String placementId, ViewGroup frame, Map<String, Object> customParameters) {
        QLog.d("starting " + placementId);

        final PlacementModel placementModel = model.getPlacement(placementId);

        if (placementModel.getState().adState != AdState.AD_READY) {
            QLog.e("wrong state of ad: " + placementModel.getState() + ", cancelling ad alltogether!");
            placementModel.notifyError("wrong state of ad: " + placementModel.getState() + ", cancelling ad alltogether!");
            return;
        }

        placementModel.setAdState(AdState.SHOWING_AD);

        frame.addView(currentWebView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        sendEvents(AdTrackingEvent.create("adStarted", placementModel.getAdResponse().adId).setCustomParameters(customParameters));
    }

    public void close(String placementId) {
        QLog.d("closing " + placementId);

        final PlacementModel placementModel = model.getPlacement(placementId);

        AdState adState = placementModel.getAdState();

        if (adState != AdState.DISMISSED && placementModel.getAdResponse() != null) {
            sendEvents(
                    AdTrackingEvent
                            .create("adDismissed", placementModel.getAdResponse().adId)
                            .internalParameter("step", placementModel.getCurrentStep())
            );
        }

        clearWebView();

        currentPlacement = null;

        switch (placementModel.getAdState()) {
            case AD_READY:
            case LOADING_AD:
            case SHOWING_AD:
            case CALL2ACTION:
            case CALL2ACTIONCLICKED:
            case GOAL_REACHED:
            case NOFILL:
                placementModel.setAdState(AdState.DISMISSED);
        }

    }

    public void sendEvents(Object... events) {
        List<Object> currentEvents = scheduledTrackingEventsProperty.get();
        for (Object event : events) {
            //scheduledTrackingEvents.add(event);
            currentEvents.add(event);
        }
        scheduledTrackingEventsProperty.set(currentEvents);
    }

    /**
     * Currently pending transactions. Do not forget to call {@link #checkForTransactions()} to update this list getFrom time to time!
     *
     * @return
     */
    public Observable<Collection<OpenTransaction>> pendingTransactions() {
        return openTransactions
                        .observe()
                        .map(this::filterActiveEvents);
    }

    private Collection<OpenTransaction> filterActiveEvents(Collection<OpenTransaction> input) {
        QLog.d("open transactions changed " + input.size());
        Set<OpenTransaction> filtered = new HashSet<>();
        for (OpenTransaction cb : input) {
            if (cb.state == OpenTransaction.State.ACTIVE)
                filtered.add(cb);
            else {
                QLog.d("filtering out " + cb);
            }
        }
        return filtered;
    }

    /**
     * @return if it worked or not. Please check the list {@link #pendingTransactions()} for the data.
     */
    public void checkForTransactions() {
        // TODO: implement
    }

    public void completeTransaction(final OpenTransaction transaction) {

        transaction.state = OpenTransaction.State.SENDING;

        TransactionConfirmedEvent send = new TransactionConfirmedEvent();
        send.adId = transaction.adId;
        send.transactionId = transaction.transactionId;

        // TODO: queue
        KwizzadApi.getInstance().send(send)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.mainThread())
                .retryWhen(errors -> errors.flatMap(error -> {
                    QLog.e("error sending transaction confirmation: " + error.getMessage());
                    return defaultErrorHandler(error);
                }))
                .subscribe(list -> {
                    transaction.state = OpenTransaction.State.SENT;

                }, error -> {
                    transaction.state = OpenTransaction.State.ERROR;
                });

    }

    public void resume(Context context) {
        /*Observable.fromCallable(() -> AdvertisingIdClient.getAdvertisingIdInfo(context))
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.mainThread())
                .flatMap(info -> {
                    if (info.isLimitAdTrackingEnabled()) {
                        QLog.d("no ad tracking enabled :(");
                        model.advertisingId = null;
                    } else {
                        model.advertisingId = info.getId();
                    }
                    return Observable.just(info);
                })
                .subscribe(
                        foo -> QLog.d("foo " + foo),
                        e -> QLog.e(e)
                );*/
    }


    public Boolean getPreloadAdsAutomatically() {
        return preloadAdsAutomatically;
    }

    public void setPreloadAdsAutomatically(Boolean preloadAdsAutomatically) {
        this.preloadAdsAutomatically = preloadAdsAutomatically;
    }
}
