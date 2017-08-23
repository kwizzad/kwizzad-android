package com.kwizzad.api;

import android.content.Context;
import android.os.Build;

import com.kwizzad.model.Gender;
import com.kwizzad.model.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tvsmiles on 17.08.17.
 */

public class QueryParams {
    public static String addCometQueryParams(String url, Model model, Context context) {
        Map<String, String> params = new HashMap<>();
        params.put("device_make", Build.MANUFACTURER);
        params.put("device_model", Build.MODEL);
        params.put("device_os", "Android");
        params.put("device_osv", Build.VERSION.RELEASE);
        params.put("app_bundle", context.getPackageName());
        params.put("app_domain", context.getPackageName());
        params.put("app_name", context.getApplicationInfo().nonLocalizedLabel.toString());
        if(model.userDataModel.getGender() != Gender.UNKNOWN) {
            params.put("gender", model.userDataModel.getGender().toString());
        }
        params.put("device_ifa", model.advertisingId);
        String playStoreLink = "http://play.google.com/store/apps/details?id=" + context.getPackageName();
        params.put("app_storeurl", playStoreLink);



        return addQueryParams(url, params);
    }

    private static String addQueryParams(String url, Map<String, String> params) {
        StringBuilder resultUrl = new StringBuilder(url);

        for (Map.Entry<String, String> nextEntry:
             params.entrySet()) {
            resultUrl.append("&").append(nextEntry.getKey()).append("=").append(nextEntry.getValue());
        }

        return resultUrl.toString();
    }
}
