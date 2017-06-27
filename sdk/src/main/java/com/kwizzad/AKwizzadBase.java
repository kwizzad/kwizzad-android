package com.kwizzad;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.kwizzad.api.HttpErrorResponseException;
import com.kwizzad.api.KwizzadApi;
import com.kwizzad.log.DebugLoggerImplementation;
import com.kwizzad.log.QLog;
import com.kwizzad.model.AdState;
import com.kwizzad.model.Model;
import com.kwizzad.model.OpenTransaction;
import com.kwizzad.model.PlacementModel;
import com.kwizzad.model.events.AEvent;
import com.kwizzad.model.events.AdRequestEvent;
import com.kwizzad.model.events.AdResponseEvent;
import com.kwizzad.model.events.AdTrackingEvent;
import com.kwizzad.model.events.NoFillEvent;
import com.kwizzad.model.events.OpenTransactionsEvent;
import com.kwizzad.model.events.TransactionConfirmedEvent;

import org.json.JSONObject;

import java.io.IOException;
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

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subjects.PublishSubject;

public class AKwizzadBase {

    private static final long TIMEOUT_REQUESTING_AD = 1000 * 10;

    private final ISchedulers schedulers;
    public final KwizzadApi api;
    private final Model model;

    private final PublishSubject<Throwable> errors = PublishSubject.create();
    private final Context applicationContext;
    private WebView currentWebView;
    private String currentPlacement;

    private Subscription lastSubscription;

    private List<Object> scheduledTrackingEvents = new ArrayList<>();

    public AKwizzadBase(Model model, ISchedulers schedulers, KwizzadApi api, Configuration configuration) {
        this.schedulers = schedulers;
        this.api = api;
        this.model = model;
        this.applicationContext = configuration.context;

        if (configuration.debug) {
            QLog.setInstance(new DebugLoggerImplementation(configuration.context, 6));
        }

        // TODO: check for change!
        model.apiKey.set(configuration.apiKey);
        if (configuration.overrideServer != null)
            model.server = configuration.overrideServer;
        model.overrideWeb = configuration.overrideWeb;

        if (model.installId.get() == null) {
            model.installId.set(UUID.randomUUID().toString());
        }
    }

    public void start() {
        api.observe(OpenTransactionsEvent.class)
                .observeOn(schedulers.mainThread())
                .subscribe(openTransactionsEvent -> {

                    QLog.d("got open transactions " + openTransactionsEvent);

                    Set<OpenTransaction> newSet = new HashSet<>();
                    for (OpenTransaction cb : model.openTransactions.get()) {
                        if (openTransactionsEvent.transactionList.contains(cb)) {
                            newSet.add(cb);
                        }
                    }

                    newSet.addAll(openTransactionsEvent.transactionList);

                    QLog.d("size " + newSet.size());

                    if (newSet.size() > 0 || newSet.size() != model.openTransactions.get().size()) {
                        QLog.d("setting transactions");
                        model.openTransactions.set(newSet);
                    }

                });

        api.observe(AdResponseEvent.class)
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

        api.observe(NoFillEvent.class)
                .observeOn(schedulers.mainThread())
                .subscribe(event -> {
                    final PlacementModel m = model.getPlacement(event.placementId);
                    if (m.getAdState() == AdState.REQUESTING_AD) {
                        m.setAdresponse(null);
                        m.setAdState(AdState.NOFILL);
                        m.retryAfter = event.retryAfter;
                    }
                });

        final List<Object> eventsSending = new ArrayList<>();

        Observable.interval(250, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .observeOn(schedulers.mainThread())
                .subscribe((bar) -> {
                    if (!scheduledTrackingEvents.isEmpty() && eventsSending.size() == 0) {
                        eventsSending.clear();
                        eventsSending.addAll(scheduledTrackingEvents);
                        StringBuilder sb = new StringBuilder("sending events: \n");
                        for (Object event : eventsSending) {
                            sb.append(event.toString());
                            sb.append("\n");
                        }
                        QLog.d(sb.toString());

                        Observable
                                .from(new ArrayList<>(eventsSending))
                                .observeOn(schedulers.io())
                                .flatMap(api::send)
                                .observeOn(schedulers.mainThread())
                                // also main thread!
                                .retryWhen(errors -> errors.flatMap(error -> {

                                    eventsSending.clear();

                                    QLog.e("error sending events: " + error.getMessage());

                                    return defaultErrorHandler(error);
                                }))
                                .subscribe(new Subscriber<AEvent>() {
                                    @Override
                                    public void onCompleted() {
                                        if (eventsSending.size() > 0) {
                                            scheduledTrackingEvents.removeAll(eventsSending);
                                            QLog.d("successfully sent " + eventsSending.size() + " tracking events");
                                            eventsSending.clear();
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        QLog.e(e, "error sending tracking events");
                                    }

                                    @Override
                                    public void onNext(AEvent aEvent) {

                                    }
                                });
                    }
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

    public Observable<Throwable> observeErrors() {
        return errors;
    }

    public void requestAd(String placementId) {
        QLog.d("request ad for " + placementId);

        final PlacementModel placementModel = model.getPlacement(placementId);

        PlacementModel.State placementState = placementModel.state.get();
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
                if (placementModel.retryAfter != null && placementModel.retryAfter.after(now)) {
                    QLog.i("no fill said to retry after " + placementModel.retryAfter + " but we have " + now + ". ignoring request for now.");
                    return;
                }
                break;
        }
        // TODO: check other states

        placementModel.setAdState(AdState.REQUESTING_AD);

        final AdRequestEvent requestEvent = new AdRequestEvent(applicationContext);
        requestEvent.placementId = placementId;

        Observable
                .fromCallable(() -> {
                    QLog.d("checking idfa");
                    try {
                        return AdvertisingIdClient.getAdvertisingIdInfo(applicationContext);
                    } catch (Exception e) {
                        QLog.e(e.getMessage());
                    }
                    return null;
                })
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.mainThread())
                .flatMap(info -> {
                    try {
                        if (info == null || info.isLimitAdTrackingEnabled()) {
                            QLog.d("no ad tracking enabled :(");
                            model.advertisingId = null;
                        } else {
                            model.advertisingId = info.getId();
                        }
                    } catch (Exception e) {
                        QLog.e(e.getMessage());
                    }
                    return Observable.just(null);
                })
                .observeOn(schedulers.io())
                .flatMap(foo -> {
                    QLog.d("requesting the ad for " + placementId);
                    requestEvent.idfa = model.advertisingId;
                    requestEvent.userDataModel = model.userDataModel.realClone();
                    return api.send(requestEvent);
                })
                .observeOn(schedulers.mainThread())
                .subscribe(new Subscriber<AEvent>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable error) {
                        QLog.e(error, "error requesting ad");
                        errors.onNext(error);
                        if (error instanceof IOException) {
                            return;
                        }
                        if (error instanceof GooglePlayServicesNotAvailableException) {
                            // we dont handle for now
                        }
                        if (error instanceof GooglePlayServicesRepairableException) {
                            // we dont handle for now
                        }
                        if (placementModel.getAdState() == AdState.REQUESTING_AD) {
                            placementModel.setAdState(AdState.DISMISSED);
                        }
                    }

                    @Override
                    public void onNext(AEvent event) {
                        QLog.d("EVENT " + event.type);
                    }
                });
    }

    public void prepare(String placementId, Activity activity) {
        final PlacementModel placementModel = model.getPlacement(placementId);

        if (activity == null || activity.getBaseContext() == null) {
            QLog.e("Application Lifecycle Error: Can only prepare ads if Activity and Context initialized!");
            return;
        }

        if (placementModel.getAdState() == AdState.RECEIVED_AD) {

            QLog.d("preparing " + placementId);

            placementModel.currentStep = 0;
            placementModel.retryAfter = null;

            clearWebView();

            if (currentWebView != null) {

                // if it was another placement, cancel the other placement
                if (!currentPlacement.equals(placementId)) {
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
                try {
                    Method mGetRawH = Display.class.getMethod("getRawHeight");
                    Method mGetRawW = Display.class.getMethod("getRawWidth");
                    realWidth = (Integer) mGetRawW.invoke(display);
                    realHeight = (Integer) mGetRawH.invoke(display);
                } catch (Exception e) {
                    realWidth = display.getWidth();
                    realHeight = display.getHeight();
                    Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
                }

            } else {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(currentWebView, true);
            }
            currentWebView.setBackgroundColor(0xff000000);


            if (lastSubscription != null) {
                lastSubscription.unsubscribe();
                lastSubscription = null;
            }
            lastSubscription = placementModel.state.observe().subscribe(state -> {
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
                        lastSubscription.unsubscribe();
                        lastSubscription = null;
                    }
                }
            });

            new PlacementJavascriptInterface(currentWebView, placementModel, this, model);

            placementModel.setAdState(AdState.LOADING_AD);

            QLog.d("load url " + placementModel.getAdresponse().url);

            currentWebView.loadUrl(placementModel.getAdresponse().url);
            final AdResponseEvent adresponse = placementModel.getAdresponse();
            new Handler().postDelayed(() -> {
                if (placementModel.getAdState() == AdState.LOADING_AD && adresponse == placementModel.getAdresponse()) {
                    QLog.w("ad loading timeout reached. this shouldnt happen, but it did. cancelling ad :(");
                    placementModel.setAdState(AdState.DISMISSED);
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

        if (placementModel.state.get().adState != AdState.AD_READY) {
            QLog.e("wrong state of ad: " + placementModel.state.get() + ", cancelling ad alltogether!");
            placementModel.setAdState(AdState.DISMISSED);
            return;
        }

        placementModel.setAdState(AdState.SHOWING_AD);

        frame.addView(currentWebView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        sendEvents(AdTrackingEvent.create("adStarted", placementModel.getAdresponse().adId).setCustomParameters(customParameters));
    }

    public void close(String placementId) {
        QLog.d("closing " + placementId);

        final PlacementModel placementModel = model.getPlacement(placementId);

        AdState adState = placementModel.getAdState();

        if (adState != AdState.DISMISSED && placementModel.getAdresponse() != null) {
            sendEvents(
                    AdTrackingEvent
                            .create("adDismissed", placementModel.getAdresponse().adId)
                            .internalParameter("step", placementModel.currentStep)
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
        for (Object event : events) {
            scheduledTrackingEvents.add(event);
        }
    }

    /**
     * Currently pending transactions. Do not forget to call {@link #checkForTransactions()} to update this list getFrom time to time!
     *
     * @return
     */
    public Observable<Collection<OpenTransaction>> pendingTransactions() {
        return
                model.openTransactions
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
        api.send(send)
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
}
