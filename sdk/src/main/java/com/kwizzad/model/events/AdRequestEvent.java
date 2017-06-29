package com.kwizzad.model.events;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.kwizzad.BuildConfig;
import com.kwizzad.db.ToJson;
import com.kwizzad.model.EPlatformType;
import com.kwizzad.model.UserDataModel;

import org.json.JSONException;
import org.json.JSONObject;

public class AdRequestEvent extends AAdEvent {

    private final Network androidNetwork;
    public EPlatformType platformType = EPlatformType.ANDROID;
    public String sdkVersion = BuildConfig.VERSION_NAME;
    public String osVersion = Build.VERSION.RELEASE + "," + android.os.Build.VERSION.SDK_INT;
    public String apiVersion = "1.0";
    public UserDataModel userDataModel;

    private static final class Network {
        public String operator;
        public String countryISO;
        public String cellNetworkType;
        public boolean available;
        public boolean connected;
        public String type;
        public String subType;

        public Network(Context context) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            try {
                operator = telephonyManager.getNetworkOperatorName() + ", " + telephonyManager.getSimOperatorName();
            } catch (Exception ignored) {
            }
            try {
                countryISO = telephonyManager.getNetworkCountryIso() + ", " + telephonyManager.getSimCountryIso();
            } catch (Exception ignored) {
            }
            try {
                switch (telephonyManager.getNetworkType()) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        cellNetworkType = "GPRS";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        cellNetworkType = "EDGE";
                        break;
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                        cellNetworkType = "CDMA";
                        break;
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                        cellNetworkType = "1xRTT";
                        break;
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        cellNetworkType = "IDEN";
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        cellNetworkType = "UMTS";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        cellNetworkType = "EVDO_0";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        cellNetworkType = "EVDO_A";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        cellNetworkType = "EVDO_B";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                        cellNetworkType = "EHRPD";
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        cellNetworkType = "HSPAP";
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                        cellNetworkType = "HSDPA";
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                        cellNetworkType = "HSUPA";
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                        cellNetworkType = "HSPA";
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        cellNetworkType = "LTE";
                        break;
                    default:
                        cellNetworkType = "Unknown";
                        break;
                }
            } catch (Exception ignored) {
            }
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                available = activeNetwork.isAvailable();
                connected = activeNetwork.isConnectedOrConnecting();
                type = activeNetwork.getTypeName();
                subType = activeNetwork.getSubtypeName();
            } catch (Exception ignored) {
            }
        }

        public void to(JSONObject o) throws JSONException {
            o.put("operator", operator);
            o.put("countryISO", countryISO);
            o.put("cellNetworkType", cellNetworkType);
            o.put("available", available);
            o.put("connected", connected);
            o.put("type", type);
            o.put("subType", subType);
        }
    }

    private final String deviceInformation;
    private final int androidApiLevel;
    private final String androidVersion;
    public String idfa;
    public String appVersion ;
    public String packageName ;

    public AdRequestEvent(Context context) {
        this.deviceInformation = Build.MANUFACTURER + " " + Build.MODEL;
        androidApiLevel = Build.VERSION.SDK_INT;
        androidVersion = Build.VERSION.RELEASE;
        androidNetwork = new Network(context);
        packageName = context.getPackageName();
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            appVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            appVersion = "";
        }
    }

    @ToJson
    @Override
    public void to(JSONObject o) throws JSONException {
        super.to(o);
        o.put("deviceInformation", deviceInformation);
        o.put("androidApiLevel", androidApiLevel);
        o.put("androidVersion", androidVersion);
        o.put("idfa", idfa);
        JSONObject o2 = new JSONObject();
        androidNetwork.to(o2);
        o.put("androidNetwork", o2);

        JSONObject userData = new JSONObject();
        o.put("userData", userData);
        userData.put("PlatformType", platformType.key);
        userData.put("sdkVersion", sdkVersion);
        userData.put("OSVersion", osVersion);
        userData.put("apiVersion", apiVersion);
        userData.put("AppVersion", appVersion);
        userData.put("PackageName", packageName);
        if (idfa != null) {
            userData.put("idfa", idfa);
        }

        userDataModel.toJson(userData);

    }
}