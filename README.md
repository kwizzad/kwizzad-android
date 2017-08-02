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
- Apps integrating KWIZZAD SDK require Android 4.1 (API level 16 Jelly Bean) or higher to run. Advertising will only be played starting from Android 4.3 (API Level 18 Jelly Bean) or higher.
- Using gradle build system
- Supports either JDK7 or JDK8
- Does not support new Jack compiler
- A fully working example can be found at: [https://github.com/kwizzad/kwizzad-android](https://github.com/kwizzad/kwizzad-android-example)

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
    compile ('com.github.kwizzad:kwizzad-android:x.y.z') {
		// exclude group:"com.android.support" // uncomment in case of android support library dependency conflicts
	}
}
```

To uniquely identify devices our SDK requires the Android Advertising ID. For collecting the Android Advertising ID and to comply with the Google Play content guidelines you have to add the Google Play Services SDK to your Android project. More information about the Google Play Services SDK can be found on [Google Developer site](https://developers.google.com/android/guides/setup).



## Resolve dependency conflicts

To avoid dependency conflicts you should always use the most recent version of any library and so does KWIZZAD.

In case of dependency conflicts between your app and KWIZZAD you can always include KWIZZAD without any dependency and explicitly use your versions. Please send us a note of your exact dependency library and version so we can perform a compatibility check.

To manually include KWIZZAD dependencies your build.gradle should look like this:

```java
dependencies {
    compile ('com.github.kwizzad:kwizzad-android:x.y.z') {
            exclude group:"com.android.support" // avoid android support library dependency conflicts
            // exclude group:  // you can exclude more potential conflicts here
            // exclude module:
            }
    }
}
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



## Step 2: Prepare and show an ad

The KWIZZAD SDK maintains an internal lifecycle for each ad and allows you to control the exact behaviour depending on your needs. Here is a simple example implementation that will prepare and show a KWIZZAD from a Fragment following an ad request.

KWIZZAD uses [reactivex](http://reactivex.io/) to implement an Observer pattern and manage the ad's lifecycle. You do not need any experience with reactivex as every aspect is taken care of.  

Kwizzad.createAdViewBuilder() supports both Fragment implementations depending on your needs:

1. supportDialogFragment() creates a fragment based on android.support.v4.app.DialogFragment
2. dialogFragment() creates a fragment based on android.app.DialogFragment

```java
@Override
public void onResume() {
    super.onResume();

    /**
     * listen to value, bound to the tag "this"
     */
    RxSubscriber.subscribe(this, Kwizzad.placementState(placementId).observe(), placementState -> {
        switch (placementState.adState) {
            case NOFILL:
                // no ad for placement. close fragment.
                try {
                    dismiss();
                } catch (Exception ignored) {
                }
                break;
            case RECEIVED_AD:
                // ad received, now prepare resources in the background
                Kwizzad.prepare("placementId as provided by KWIZZAD publisher integration team", getActivity());
                break;
            case AD_READY:
                // ad ready to show for placement
                Kwizzad
                        .createAdViewBuilder()
                        /*
                         * dont forget to set the placement id
                         */
                        .setPlacementId(placementId)
                        /*
                         * set any custom parameters like transactionId, aff_sub, tracking token, ...
                         * These will be available later on in callbacks to identify the originating ad session.
                         */
                        .setCustomParameter("foo", "bar")
                        .setCustomParameter("transactionId", "0815")
                        /*
                         * build it
                         */
                         .dialogFragment()
                        /*
                         * and show it
                         */
                            .show(getFragmentManager(), "ad");
                break;
            case DISMISSED:
                // finished showing the ad for placement. close fragment.
                try {
                    dismiss();
                } catch (Exception ignored) {
                }
                break;
            default:
                // loading the ad for placement.
                break;
        }
    });
}

@Override
public void onPause() {
    super.onPause();


    /**
     * unsubscribe everything on the tag "this"
     */
    RxSubscriber.unsubscribe(this);
}

@Override
public void onDestroy() {
    super.onDestroy();

    Kwizzad.close(placementId);
}
```



# Step 3: Handle callback events for completed ads

Once the user successfully completes a rewarded KWIZZAD you will receive a callback. This serves as a confirmation that you will be paid for the ad and you can reward the user accordingly.

Callbacks can be handled both in app or using KWIZZADs server2server postback configuration as an HTTP GET request to a URL of your choice.

In order to receive callbacks inapp you must subscribe to the Kwizzad.pendingTransactions() observable or use Kwizzad.setPendingTransactionsCallback() to receive notifications on new callbacks. If you are only using server2server callbacks you do not have to implement this.

There are two types of events available and defined in com.kwizzad.model.Type:

1. CALL2ACTION: Notification is sent immediately if a user played a WKIZZAD. This is relevant for instant-rewards with CPM billing based publisher agreements.
2. CALLBACK: Notification is sent later if an advertising partner confirms a transaction. This is the main billing event.

```java
import com.kwizzad.Kwizzad;
import com.kwizzad.model.PendingEvent;
import com.kwizzad.property.RxSubscriber;

public class MainActivity extends AppCompatActivity {
    @Override
protected void onResume() {
    super.onResume();

    Kwizzad.resume(this);

    nextPendingTransaction();
}

private void nextPendingTransaction() {
    pendingSubscription = Kwizzad.pendingTransactions()
            .filter(openTransactions -> openTransactions!=null && openTransactions.size()>0)
            .flatMap(openTransactions -> {
                // only return sth if we find an active one
                for(OpenTransaction openTransaction : openTransactions)
                    if(openTransaction.state == OpenTransaction.State.ACTIVE)
                        return Observable.just(openTransaction);

                //nothing
                return Observable.empty();
            })
            .first()
            .subscribe(openTransaction -> {
                QLog.d("should show event " + openTransaction);
                showEvent(openTransaction);
            });
}

private void showEvent(final OpenTransaction openTransaction) {

    Reward reward = openTransaction.reward;
    if (reward != null) {
        // Here you get the reward amount for the user.
        int rewardAmount = reward.amount;
        String rewardCurrency = reward.currency; // e.g. chips, coins, loot, smiles as configued by KWIZZAD.

        /*

        The following part is optional: If you want to know which type of reward the notification was about
        How to distinguish between Call2Action (Instant-Reward) and Callback (full billing)

        Reward.Type rewardType = reward.type; // CALL2ACTIONSTARTED, CALLBACK, GOALREACHED

        if (openTransaction.reward.type.equals(Reward.Type.CALL2ACTIONSTARTED)) { ... }

        else if (openTransaction.reward.type.equals(Reward.Type.CALLBACK)) { ... }

      */
    }

    new AlertDialog.Builder(this)
            .setTitle("Transaction!")
            .setMessage(openTransaction.toString())
            .setPositiveButton(android.R.string.ok,
                    (dialog, whichButton) -> Kwizzad.completeTransaction(openTransaction)
            )
            .setNegativeButton(android.R.string.cancel,
                    (dialog, whichButton) -> {
                        dialog.dismiss();
                    }
            )
            // get the next one
            .setOnDismissListener(dialog -> nextPendingTransaction())
            .create()
            .show();
}

@Override
protected void onPause() {
    super.onPause();
    RxSubscriber.unsubscribe(this);
    if(pendingSubscription!=null) {
        pendingSubscription.unsubscribe();
    }
}
```
