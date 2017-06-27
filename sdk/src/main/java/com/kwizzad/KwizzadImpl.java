package com.kwizzad;

import android.os.Build;
import android.webkit.WebView;

import com.kwizzad.api.KwizzadApi;
import com.kwizzad.log.QLog;
import com.kwizzad.model.Model;

public class KwizzadImpl extends AKwizzadBase {

    public KwizzadImpl(Model model, ISchedulers schedulers, KwizzadApi api, Configuration configuration) {
        super(model, schedulers, api, configuration);

        if (BuildConfig.DEBUG) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                QLog.e("----------------------------------------");
                QLog.e("enabling web contents debugging! DO NOT RELEASE WITH THIS ENABLED!");
                QLog.e("----------------------------------------");
                WebView.setWebContentsDebuggingEnabled(true);
            }

        }
    }
}
