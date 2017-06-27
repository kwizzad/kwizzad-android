package com.kwizzad.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.kwizzad.Kwizzad;
import com.kwizzad.log.QLog;
import com.kwizzad.model.AdState;
import com.kwizzad.property.RxSubscriber;

import java.util.HashMap;
import java.util.Map;

public class SimpleActivity extends AppCompatActivity {

    private String placementId;
    private View adButton;
    private TextView statusTextView;

    public static Intent createIntent(Context context, String placementId) {
        Intent intent = new Intent(context, SimpleActivity.class);
        intent.putExtra("PLACEMENT_ID", placementId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.a_ad);

        placementId = getIntent().getStringExtra("PLACEMENT_ID");

        adButton = findViewById(R.id.adButton);

        statusTextView = ((TextView) findViewById(R.id.statusText));

        adButton.setOnClickListener(v -> {

            Map<String, Object> customParams = new HashMap<>();
            customParams.put("foo", "bar");

            Kwizzad
                    .createAdViewBuilder()
                    /*
                     * dont forget to set the placement id
                     */
                    .setPlacementId(placementId)
                    /*
                     * like this
                     */
                    .setCustomParameters(customParams)
                    /*
                     * or like this
                     */
                    .setCustomParameter("bar", "foo")
                    /*
                     * build it
                     */
                    .dialogFragment()
                    /*
                     * and show it
                     */
                    .show(getFragmentManager(), "ad");
        });

        /**
         * now we are requesting an ad for the placement.
         */
        Kwizzad.requestAd(placementId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * listen to value, bound to the tag "this"
         */
        RxSubscriber.subscribe(this, Kwizzad.getPlacementModel(placementId).observeState(), placementState -> {

            QLog.d("got state " + placementState.adState);

            switch (placementState.adState) {
                case NOFILL:
                    statusTextView.setText("no ad for placement " + placementId);

                    new AlertDialog.Builder(SimpleActivity.this)
                            .setTitle("NOFILL")
                            .setMessage("there is no ad. sorry")
                            .setNeutralButton(android.R.string.ok, (dialog, which) -> finish())
                            .create()
                            .show();

                    break;
                case RECEIVED_AD:
                    statusTextView.setText("received an ad for the placement " + placementId);
                    adButton.setVisibility(View.VISIBLE);
                    break;
                case DISMISSED:
                    /**
                     * we can continue to do whatever we want to do after
                     */
                    finish();

                    break;
                default:
                    statusTextView.setText("requesting an ad for the placement " + placementId);
                    adButton.setVisibility(View.GONE);
            }


        });

        /**
         * we can also just get the value
         */
        AdState state = Kwizzad.getPlacementModel(placementId).getState().adState;
    }

    @Override
    protected void onPause() {
        super.onPause();

        /**
         * unsubscribe everything on the tag "this"
         */
        RxSubscriber.unsubscribe(this);
    }
}
