package com.kwizzad.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.kwizzad.IPlacementModel;
import com.kwizzad.Kwizzad;
import com.kwizzad.KwizzadErrorCallback;
import com.kwizzad.PendingTransactionsCallback;
import com.kwizzad.model.OpenTransaction;
import com.kwizzad.model.events.Reward;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Created by tvsmiles on 12.05.17.
 */

public class ExampleActivity extends AppCompatActivity {

    private final static String TAG = "ExampleActivity";

    private Button btnPreload;
    private EditText etPlacement;
    private AdView adView;
    private TextView tvLog;
    private CheckBox cbPreloadAutomatically;

    IPlacementModel.PlacementStateCallback placementStateCallback;
    KwizzadErrorCallback errorCallback;
    PendingTransactionsCallback transactionsCallback;

    //we need this param to avoid showing rewards dialog several time
    private boolean rewardsAreShown = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_example_activity_layout);

        init();

        Kwizzad.setPreloadAdsAutomatically(false);

        /*
         * set callback to state changes
         */
        setCallbackToStateChanges();

        /*
         * add callback for rewards
         */
        transactionsCallback = transactions -> {
            if(transactions != null && transactions.size() > 0) {
                showEvents(transactions);
            }
        };
        Kwizzad.setPendingTransactionsCallback(transactionsCallback);


        /*
         * now we are requesting an ad for the placement.
         */
        Kwizzad.requestAd(etPlacement.getText().toString());

        /*
         * set an error callback
         */
        errorCallback = throwable -> {
            tvLog.setText("got error " + throwable.getMessage() + " \n" + tvLog.getText());
        };

        Kwizzad.getPlacementModel(etPlacement.getText().toString()).setErrorCallback(errorCallback);

        adView.setLoading();
    }

    private void init() {
        btnPreload = (Button) findViewById(R.id.btn_preload);
        etPlacement = (EditText) findViewById(R.id.et_placement);
        adView = (AdView) findViewById(R.id.av);
        tvLog = (TextView) findViewById(R.id.tv_log);
        cbPreloadAutomatically = (CheckBox) findViewById(R.id.cb_preload_param);

        cbPreloadAutomatically.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Kwizzad.setPreloadAdsAutomatically(isChecked);
        });


        adView.setOnClickListener(v -> {

            Intent intent = Kwizzad.createAdViewBuilder()
                    .setPlacementId(etPlacement.getText().toString())
                    .activityIntent(this);
            startActivity(intent);
        });

        btnPreload.setOnClickListener(v -> {
            tvLog.setText("requesting ad for placement " + etPlacement.getText() + "\n\n" + tvLog.getText());
            adView.setLoading();
            hideKeyboard();

            /*
             * resubscribing to state changes because placement could be changed
             */
            setCallbackToStateChanges();

            Kwizzad.requestAd(etPlacement.getText().toString());

            /*
             * set an error callback
             */
            Kwizzad.getPlacementModel(etPlacement.getText().toString()).setErrorCallback(throwable -> {
                tvLog.setText("got error " + throwable.getMessage() + " \n" + tvLog.getText());
            });
        });
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if(getCurrentFocus() != null) {
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Kwizzad.resume(this);
    }

    private void setCallbackToStateChanges() {
        placementStateCallback = state -> {
            Log.d(TAG, "got state " + state + " \n" + tvLog.getText());
            tvLog.setText("got state " + state + " \n" + tvLog.getText());

            switch (state) {
                case NOFILL:
                    Log.d(TAG, "no ad for placement " + etPlacement.getText().toString());
                    tvLog.setText("no ad for placement " + etPlacement.getText().toString() + "\n" + tvLog.getText());
                    new AlertDialog.Builder(this)
                            .setTitle("NOFILL")
                            .setMessage("there is no ad. sorry")
                            .setNeutralButton(android.R.string.ok, (dialog, which) -> {})
                            .create()
                            .show();
                    break;
                case RECEIVED_AD:
                    // Calculate the total reward amount. We can show this to the app user before starting the actual KWIZZAD.
                    Log.d(TAG, "ad ready to show for placement " + etPlacement.getText().toString());
                    tvLog.setText("ad ready to show for placement " + etPlacement.getText().toString() + "\n" + tvLog.getText());

                    adView.setPlacementModel(Kwizzad.getPlacementModel(etPlacement.getText().toString()));
                    Kwizzad.prepare(etPlacement.getText().toString(), this);
                    break;
                case AD_READY:
                    rewardsAreShown = false;
                    break;
                case DISMISSED:

                    Log.d(TAG, "finished showing the ad for placement " + etPlacement.getText().toString());
                    tvLog.setText("finished showing the ad for placement " + etPlacement.getText().toString() + "\n" + tvLog.getText());

                    adView.setDismissed();
                    break;
                default:
                    Log.d(TAG, "unhandled state " + state);
                    break;
            }
        };

        Kwizzad.getPlacementModel(etPlacement.getText().toString()).setPlacementStateCallback(placementStateCallback);
    }


    private void showEvents(Collection<OpenTransaction> openTransactions) {
        if(!rewardsAreShown) {
            rewardsAreShown = true;
            List<Reward> rewards = new ArrayList<>();
            for (OpenTransaction transaction :
                    openTransactions) {
                rewards.add(transaction.reward);
            }

            String message = Reward.makeConfirmationTextForRewards(this, rewards);

            new AlertDialog.Builder(this)
                    .setTitle("New rewards!")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, whichButton) -> Kwizzad.completeTransactions(openTransactions)
                    )
                    .create()
                    .show();
        }
    }

}
