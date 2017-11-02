package com.kwizzad;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kwizzad.api.KwizzadApi;
import com.kwizzad.api.Request;
import com.kwizzad.log.QLog;
import com.kwizzad.model.AdState;
import com.kwizzad.model.Model;
import com.kwizzad.model.events.AdTrackingEvent;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * if (typeof window.handleKwizzadJIEvent === 'function') {
 * window.handleKwizzadJIEvent('adDialogDisplayed', true);
 * }
 */
public class PlacementJavascriptInterface {

    private final WebView webView;
    private final AKwizzadBase kwizzad;
    private final PlacementModel placementModel;

    private boolean goalUrlCondition = true;
    private boolean dismissOnGoalUrl = true;
    private Handler handler = new Handler();
    private boolean goalReached;

    public PlacementJavascriptInterface(WebView webView, PlacementModel placementModel, AKwizzadBase kwizzad) {
        this.placementModel = placementModel;

        this.webView = webView;
        this.kwizzad = kwizzad;
        //applyGoalUrl(placementModel.adresponse.goalUrlPattern);

        webView.addJavascriptInterface(this, "KwizzAdJI");
        QLog.d("added javascript interface");
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(view.getContext());
                WebView.WebViewTransport transport = (WebView.WebViewTransport)resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {

            // TODO: new
            /*@Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }*/

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                QLog.d("load " + url + ", goalurl: " + placementModel.getGoalUrl());
                if (placementModel.getGoalUrl() != null && url.contains(placementModel.getGoalUrl()) == goalUrlCondition) {
                    if (placementModel.getAdState() == AdState.SHOWING_AD
                            || placementModel.getAdState() == AdState.CALL2ACTION
                            || placementModel.getAdState() == AdState.CALL2ACTIONCLICKED) {
                        goalReached = true;

                        if (dismissOnGoalUrl) {
                            QLog.d("goal reached and dismissing " + url);

                            if (placementModel.getAdState() != AdState.DISMISSED && placementModel.getAdResponse() != null) {
                                kwizzad.sendEvents(
                                        AdTrackingEvent
                                                .create("adDismissed", placementModel.getAdResponse().adId)
                                                .internalParameter("step", placementModel.getCurrentStep())
                                );
                            }

                            placementModel.setAdState(AdState.DISMISSED);
                        } else {
                            QLog.d("goal reached but not dismissing " + url);
                        }
                    }
                }

                if (url.startsWith("intent://")) {

                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Intent existPackage = view.getContext().getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                        if (existPackage != null) {
                            Activity host = (Activity) view.getContext();
                            host.startActivity(intent);
                        } else {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                            Activity host = (Activity) view.getContext();
                            host.startActivity(marketIntent);
                        }
                        return true;
                    } catch (Exception e) {
                        QLog.e(e );
                    }

                }

                String scheme = Uri.parse(url).getScheme();

                if (scheme.equals("market")
                        || url.startsWith("http://play.google.com")
                        || url.startsWith("https://play.google.com")) {
                    try {
                        placementModel.setAdState(AdState.DISMISSED);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        Activity host = (Activity) view.getContext();
                        host.startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        QLog.e(e);
                        // Google Play app is not installed, you may want to open the app store link
                        Uri uri = Uri.parse(url);
                        view.loadUrl("http://play.google.com/store/apps/" + uri.getHost() + "?" + uri.getQuery());
                        return false;
                    }

                }

                if (scheme.equals("xhttp") || scheme.equals("xhttps")) {
                    if (url.endsWith(".ics")) {
                        Observable
                                .fromCallable(() -> new Request.Builder()
                                        .url(url.substring(1))
                                        .method("GET", null)
                                        .build())
                                .flatMap(KwizzadApi.getInstance()::send)
                                .flatMap(KwizzadApi.getInstance()::isValidResponse)

                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())

                                .subscribe(response -> {
                                    Activity host = (Activity) view.getContext();
                                    try {
                                        host.startActivity(new CalendarParse().createIntent(response.body()));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });

                        placementModel.setAdState(AdState.DISMISSED);

                        return true;
                    }
                    else {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url.substring(1)));
                            Activity host = (Activity) view.getContext();
                            host.startActivity(intent);
                            placementModel.setAdState(AdState.DISMISSED);
                        } catch (Exception e) {
                            QLog.e(e);
                        }
                    }
                }

                placementModel.pageStarted(url);

                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                QLog.d("error " + error);
                //subject.set(QuizState.ERROR);
            }

            /* These raise exception below API 23, therefore disabled for now.
             * https://developer.android.com/reference/android/webkit/WebViewClient.html#onReceivedError(android.webkit.WebView, android.webkit.WebResourceRequest, android.webkit.WebResourceError)

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                QLog.d("error " + request + ":" + error);
                //subject.set(QuizState.ERROR);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                QLog.d("error " + request + ":" + errorResponse);
                //subject.set(QuizState.ERROR);
            }

            */

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                placementModel.pageFinished(url);

                // stop timer

                if (goalReached) {
                    QLog.d("page finished after goal reached " + url);

                    placementModel.setAdState(AdState.GOAL_REACHED);

                    kwizzad.sendEvents(AdTrackingEvent.create("goalReached", placementModel.getAdResponse().adId));

                    goalReached = false;
                }
            }
        });
        QLog.d("added webview client");

    }

    private void applyGoalUrl(String goalUrlPattern) {
        QLog.d("applying goal url " + goalUrlPattern);
        if (goalUrlPattern != null) {
            this.dismissOnGoalUrl = true;

            if (goalUrlPattern.startsWith("OK")) {
                goalUrlPattern = goalUrlPattern.substring(2);
                this.dismissOnGoalUrl = false;
            }

            this.goalUrlCondition = true;
            if (goalUrlPattern.startsWith("!")) {
                goalUrlPattern = goalUrlPattern.substring(1);
                this.goalUrlCondition = false;
            }

            placementModel.setGoalUrl(goalUrlPattern);
        }
    }

    @JavascriptInterface
    public void challengeReady(int num) {
        QLog.d("jsi: challenge placementState: " + num);
        if (placementModel.getAdResponse() == null) return;

        handler.post(() -> {
            kwizzad.sendEvents(AdTrackingEvent.create("adLoaded", placementModel.getAdResponse().adId));
        });

        handler.postDelayed(() -> {
            if (placementModel.getAdState() == AdState.LOADING_AD) {
                placementModel.setAdState(AdState.AD_READY);
            }
        }, 250);

    }

    @JavascriptInterface
    public void call2Action() {
        QLog.d("jsi: call2Action");
        handler.post(() -> {
            if (placementModel.getAdState() == AdState.SHOWING_AD) {

                placementModel.setAdState(AdState.CALL2ACTION);

            }
        });
    }

    @JavascriptInterface
    public void call2ActionClicked() {
        QLog.d("jsi: call2ActionClicked");
        handler.post(() -> {
            if (placementModel.getAdState() == AdState.CALL2ACTION) {

                placementModel.setAdState(AdState.CALL2ACTIONCLICKED);

            }
        });
    }

    @JavascriptInterface
    public void challengeCompleted(int num) {
        QLog.d("jsi: challengeCompleted" + num);
        if (num >= 0) {
            handler.post(() -> {
                QLog.d("challenge completed, current step " + num);
                kwizzad.sendEvents(AdTrackingEvent.create("challenge" + num + "completed", placementModel.getAdResponse().adId));
                placementModel.setCurrentStep(num + 1);
            });
        }
    }

    @JavascriptInterface
    public void goalurl(String goalUrl) {
        QLog.d("jsi: goalurl: " + goalUrl);

        applyGoalUrl(goalUrl);
    }

    @JavascriptInterface
    public void finished() {
        QLog.d("jsi: finished");
        handler.post(() -> {
            switch (placementModel.getAdState()) {
                case SHOWING_AD:
                case CALL2ACTION:
                case CALL2ACTIONCLICKED:
                case GOAL_REACHED:
                    placementModel.setAdState(AdState.DISMISSED);
                    break;
                default:
                    placementModel.setAdresponse(null);
                    placementModel.notifyError("internal error");
                    break;
            }
        });
    }

    @JavascriptInterface
    public void detach() {

        webView.setBackgroundColor(0xffffffff);

        QLog.d("jsi: detach");
        handler.post(() -> webView.removeJavascriptInterface("KwizzAdJI"));
    }

}
