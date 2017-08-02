package com.kwizzad.example;

import android.content.Context;
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

import com.kwizzad.Kwizzad;
import com.kwizzad.model.OpenTransaction;
import com.kwizzad.model.events.Reward;
import com.kwizzad.property.RxSubscriber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Subscription;


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

    //we need this param to avoid showing rewards dialog several time
    private boolean rewardsAreShown = false;

    private Subscription pendingSubscription;
    private Subscription stateSubscription;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_example_activity_layout);

        init();

        Kwizzad.setPreloadAdsAutomatically(false);

        /*
         * subscribe to errors
         */
        RxSubscriber.subscribe(this, Kwizzad.observeErrors(), throwable -> {
            tvLog.setText("observed error " + throwable.getMessage() + " \n" + tvLog.getText());
        });

        /*
         * or set an error callback
         */
        Kwizzad.setErrorsCallback(throwable -> {
            tvLog.setText("got error " + throwable.getMessage() + " \n" + tvLog.getText());
        });

        /*
         * subscribe to state changes
         */
        resubscribeToStateChanges();

        /*
         *  or set callback for state changes
         */
        Kwizzad.getPlacementModel(etPlacement.getText().toString()).setPlacementStateCallback(state -> {
            tvLog.setText("got callback for state " + state + "\n" + tvLog.getText());
        });



        /*
         * add callback for rewards
         */
        Kwizzad.setPendingTransactionsCallback(transactions -> {
            if(transactions != null && transactions.size() > 0) {
                showEvents(transactions);
            }
        });
        /*
         * also you can observe rewards
         */
        /*
        pendingSubscription = Kwizzad.pendingTransactions()
                .filter(openTransactions -> openTransactions != null && openTransactions.size() > 0)
                .subscribe(this::showEvents);
                */



        /*
         * now we are requesting an ad for the placement.
         */
        Kwizzad.requestAd(etPlacement.getText().toString());

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
            Map<String, Object> customParams = new HashMap<>();
            customParams.put("foo", "bar");
            Kwizzad
                    .createAdViewBuilder()
                                /*
                                 * dont forget to set the placement id
                                 */
                    .setPlacementId(etPlacement.getText().toString())
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

        btnPreload.setOnClickListener(v -> {
            tvLog.setText("requesting ad for placement " + etPlacement.getText() + "\n\n" + tvLog.getText());
            adView.setLoading();
            hideKeyboard();


            // we can add callbacks for states
            Kwizzad.getPlacementModel(etPlacement.getText().toString()).setPlacementStateCallback(state -> {
                tvLog.setText("got callback for state " + state + "\n" + tvLog.getText());
            });

            /*
             * resubscribing to state changes because placement could be changed
             */
            resubscribeToStateChanges();

            Kwizzad.requestAd(etPlacement.getText().toString());


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

    private void resubscribeToStateChanges() {
        if(stateSubscription != null) {
            stateSubscription.unsubscribe();
        }
        /*
          listen to states changes
         */
        stateSubscription = Kwizzad.getPlacementModel(etPlacement.getText().toString()).observeState()
                /*
                 * we skip first element because first element is current state
                 * that we receive immediately after subscription
                 * but here we want to receive only state changes
                 */
                .skip(1)
                .subscribe(state -> {

                    Log.d(TAG, "observed state " + state.adState + " \n" + tvLog.getText());
                    tvLog.setText("observed state " + state.adState + " \n" + tvLog.getText());

                    switch (state.adState) {
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
                            Log.d(TAG, "unhandled state " + state.adState);
                            break;
                    }
                });
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




    @Override
    protected void onDestroy() {
        super.onDestroy();

        /**
         * unsubscribe everything on the tag "this"
         */
        RxSubscriber.unsubscribe(this);


        if(stateSubscription != null) {
            stateSubscription.unsubscribe();
        }


        if(pendingSubscription != null) {
            pendingSubscription.unsubscribe();
        }
    }
}
