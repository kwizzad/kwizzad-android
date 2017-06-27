package com.kwizzad.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.kwizzad.Kwizzad;

import rx.Subscription;

public class SimplePreloadingExample extends AppCompatActivity {

    private String placementId;
    private Subscription subscription;

    public static Intent createIntent(Context context, String placementId) {
        Intent intent = new Intent(context, SimplePreloadingExample.class);
        intent.putExtra("PLACEMENT_ID", placementId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        placementId = getIntent().getStringExtra("PLACEMENT_ID");

        /**
         * first we need to request the placement ad. this might take a tiny bit
         * as we request it from the server.
         */
        Kwizzad.requestAd(placementId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        subscription = Kwizzad.getPlacementModel(placementId)
                .observeState()
                .subscribe(placementState -> {
                    switch (placementState.adState) {
                        case NOFILL:

                            /*
                             * there was no ad, we just go back
                             */
                            finish();

                            break;
                        case RECEIVED_AD:

                            /**
                             * once we have received an ad, we can tell kwizzad to preload (prepare) this ad for viewing
                             */

                            Kwizzad.prepare(placementId, this);

                            break;

                        case AD_READY:

                            /**
                             * when the ad is ready, we can tell kwizzad to show it now.
                             * There are 2 ways to do it:
                             * - AdDialogFragment
                             * - AdActivity
                             * you can choose what suits you best.
                             * As we are able to display fragments in this activity and
                             * dont need to exit here, AdDialogFragment is a good choice
                             */

                            Kwizzad
                                    .createAdViewBuilder()
                                    .setPlacementId(placementId)
                                    .dialogFragment()
                                    .show(getFragmentManager(), "ad");

                            break;

                        case DISMISSED:

                            /**
                             * when the ad is dismissed, we can just simply go back
                             */

                            finish();

                            break;
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

        subscription.unsubscribe();

    }
}
