<img align="center" src="https://kwizzad.com/assets/kwizzad_logo-ea8ef9f88e2dd51829c0497740a4f190ad1821acdbec71bef32d47d458143549.svg" alt="" width="40" height="40"> Kwizzad SDK for Android
====================
[![](https://jitpack.io/v/kwizzad/kwizzad-android.svg)](https://jitpack.io/#kwizzad/kwizzad-android)

- [Prerequisites](#prerequisites)
- [Installation](#add-the-kwizzad-sdk-to-your-android-project)
- [Dependency conflicts](#resolve-dependency-conflicts)
- [Initializing](#initialize-the-sdk)
- [Integration](#quick-integration-request-and-show-an-ad)

# Prerequisites

- You already have your own KWIZZAD API KEY and PLACEMENT ID. If not, please contact TVSMILES per [E-Mail](mailto:it@tvsmiles.de) and we will register your APP.
- Apps integrating KWIZZAD SDK require Android 4.1 (API level 16 Jelly Bean) or higher to run. Advertising will only be played starting from Android 4.4 (API Level 19 Kitkat) or higher.
- Using gradle build system
- Supports either JDK7 or JDK8
- Does not support new Jack compiler
- A fully working example can be found at: [https://github.com/kwizzad/kwizzad-android](https://github.com/kwizzad/kwizzad-android)

**ProGuard configuration**

For release builds to work correctly, please ensure Kwizzad SDK's class names are kept intact using this configuration line:

```
-keep class com.kwizzad.** { *; }
```

More information about using ProGuard can be found on [Android Tools Project Site, Running ProGuard](http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Running-ProGuard).



# Add the KWIZZAD SDK to your Android project

Open the `build.gradle` file of your Android project. Add a new build rule under `repositories` and `dependencies.`

```java
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    api 'com.github.kwizzad:kwizzad-android:x.y.z'
}
```
Important: KWIZZAD requires rxjava 2 at least as runtime only dependency. So if you don't use rxjava 2 in your project use 'runtimeOnly', otherwise you can use 'api' or 'implementation'   
To uniquely identify devices our SDK requires the Android Advertising ID. For collecting the Android Advertising ID and to comply with the Google Play content guidelines you have to add the Google Play Services SDK to your Android project. More information about the Google Play Services SDK can be found on [Google Developer site](https://developers.google.com/android/guides/setup).



## Resolve dependency conflicts

To avoid dependency conflicts you should always use the most recent version of any library and so does KWIZZAD.
Inside KWIZZAD we use libraries:
- com.android.support:appcompat
- com.google.android.gms:play-services-basement
- io.reactivex.rxjava2:rxjava
- io.reactivex.rxjava2:rxandroid

And all of them marked with 'implementation' so if you dont use any of these libraries add them with 'runtimeOnly'.

```
    runtimeOnly 'com.google.android.gms:play-services-basement:11.4.2'
    runtimeOnly 'io.reactivex.rxjava2:rxjava:2.1.5'
    runtimeOnly 'io.reactivex.rxjava2:rxandroid:2.0.1'
```

# Initialize the SDK

The following should be done in your Application Class, as it should only be done once per start of the application. Please do not do this in every MainActivity. Application is really the right place here.

```java
package com.mycompany.example;

import android.app.Application;
import com.kwizzad.Configuration;
import com.kwizzad.Kwizzad;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Kwizzad.init(
            new Configuration.Builder()
                .applicationContext(this)
                .apiKey("your api key as received from the KWIZZAD publisher integration team")
                .build()
        );


        // Optional: here you can set known user data that will improve KWIZZAD's targeting capabilities.
        // You can set these at any time when more information becomes available.
        Kwizzad.getUserData().setUserId("12345");
        Kwizzad.getUserData().setGender(Gender.MALE);
        Kwizzad.getUserData().setName("Horst Guenther");
    }
}
```

# Quick Integration: Request and show an ad

## Step 1: Request an ad

You should request an ad first from our servers, so you can be sure, that there is something to show the user. A good point to do this either:

- As the last step upon app start initialization
- When the user starts an activity that will likely lead to a KWIZZAD later on. E.g. start of new level in a game if the user will be shown a KWIZZAD after or at some point during the game level
- When the user enters the in app store to purchase virtual goods.

```java
import com.kwizzad.Kwizzad;

@Override
public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    /**
     * now we are requesting an ad for the placement.
     */
    Kwizzad.requestAd("placementId as provided by KWIZZAD publisher integration team");
}
```

Notice that by default ads are rerequested immediatelly after getting DISMISSED state, and after several minutes for AD_READY and NO_FILL states. If you want to override this behaviour you can use ```Kwizzad.setPreloadAdsAutomatically(false)``` to disable standart preloading behaviour




## Step 2: Prepare and show an ad

The KWIZZAD SDK maintains an internal lifecycle for each ad and allows you to control the exact behaviour depending on your needs. Here is a simple example implementation that will prepare a KWIZZAD to show an ad.

```java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ...
    placementStateCallback = state -> {
        switch (state) {
            case NOFILL:
                //no ad for placement, show error
                new AlertDialog.Builder(this)
                    .setTitle("NOFILL")
                    .setMessage("there is no ad. sorry")
                    .setNeutralButton(android.R.string.ok, (dialog, which) -> {})
                    .create()
                    .show();
                break;
            case RECEIVED_AD:
                // ad received, now prepare resources in the background
                Kwizzad.prepare(placementId, activity);
                break;
            case AD_READY:
                // ad ready to show for placement
                rewardsAreShown = false;
                break;
            case DISMISSED:
                //ad dismissed
                break;
            default:
                break;
        }
    };
    Kwizzad.getPlacementModel(placementId).setPlacementStateCallback(placementStateCallback);
}

@Override
public void onDestroy() {
    super.onDestroy();
	Kwizzad.getPlacementModel(placementId).setPlacementStateCallback(null);
    Kwizzad.close(placementId);
}
```
Notice that all callbacks in KWIZZAD are save in weak references to avoid memory leaks. So it is important to keep reference to callback, to avoid unexpected behaviour.
Now to show the ad you have to use ```Kwizzad.createAdViewBuilder()```, that supports both Fragment implementations depending on your needs:

1. supportDialogFragment() creates a fragment based on android.support.v4.app.DialogFragment
2. dialogFragment() creates a fragment based on android.app.DialogFragment

```java
    Map<String, Object> customParams = new HashMap<>();
    customParams.put("foo", "bar");
    Kwizzad.createAdViewBuilder()
        /*
        * dont forget to set the placement id
        */
        .setPlacementId(etPlacement.getText().toString())
        /*
        * you can set custom parameters like this
        */
        .setCustomParameters(customParams)
        /*
        * or like this
        */
        .setCustomParameter("bar", "foo")
        /*
        * build it using dialog or support dialog
        */
        .dialogFragment() //.supportDialogFragment()
        /*
        * and show it
        */
        .show(getFragmentManager(), "ad");
```

# Step 3: Handle callback events for completed ads

Once the user successfully completes a rewarded KWIZZAD you will receive a callback. This serves as a confirmation that you will be paid for the ad and you can reward the user accordingly.

Callbacks can be handled both in app or using KWIZZADs server2server postback configuration as an HTTP GET request to a URL of your choice.

In order to receive callbacks inapp use ```Kwizzad.setPendingTransactionsCallback(...)```. If you are only using server2server callbacks you do not have to implement this.

There are two types of events available and defined in com.kwizzad.model.Type:

1. CALL2ACTION: Notification is sent immediately if a user played a KWIZZAD. This is relevant for instant-rewards with CPM billing based publisher agreements.
2. CALLBACK: Notification is sent later if an advertising partner confirms a transaction. This is the main billing event.

```java
import com.kwizzad.Kwizzad;
import com.kwizzad.model.PendingEvent;

public class MainActivity extends AppCompatActivity {

    PendingTransactionsCallback transactionsCallback;
    
    @Override
    protected void onResume() {
        super.onResume();

        /*
        * add callback for rewards
        */
        transactionsCallback = transactions -> {
            if(transactions != null && transactions.size() > 0) {
                showEvents(transactions);
            }
        };
        Kwizzad.setPendingTransactionsCallback(transactionsCallback);

        Kwizzad.resume(this);
    }

    private void showEvents(Collection<OpenTransaction> openTransactions) {
        List<Reward> rewards = new ArrayList<>();
        for (OpenTransaction transaction :
            openTransactions) {
            rewards.add(transaction.reward);
        }

        //test for dialog, explainig rewards
        String message = Reward.makeConfirmationTextForRewards(this, rewards);

        new AlertDialog.Builder(this)
            .setTitle("New rewards!")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok,
                (dialog, whichButton) -> Kwizzad.completeTransactions(openTransactions))
            .create()
            .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
            Kwizzad.setPendingTransactionsCallback(null);
    }
}
```

# Step 4: Handle errors in placements

Once you receive an error inside a placement, your app will be notified in error callback. As soon as you receive an error, the ads state is set to DISMISSED. To set error callback use

```java
    errorCallback = throwable -> Log.e(TAG, throwable.getMessage());
    Kwizzad.getPlacementModel(placementId).setErrorCallback(errorCallback);
```