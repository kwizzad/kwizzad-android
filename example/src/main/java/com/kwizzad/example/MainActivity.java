package com.kwizzad.example;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.kwizzad.CalendarParse;
import com.kwizzad.Kwizzad;
import com.kwizzad.log.QLog;
import com.kwizzad.model.OpenTransaction;
import com.kwizzad.property.RxSubscriber;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

public class MainActivity extends AppCompatActivity {
    private TextInputLayout placementIdInput;
    private Subscription pendingSubscription;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.a_main);

        placementIdInput = (TextInputLayout) findViewById(R.id.placementId);

        View simpleButton = findViewById(R.id.simple);
        if (simpleButton != null) {
            simpleButton.setOnClickListener(v -> {
                if (Kwizzad.isInitialized()) {
                    startActivity(SimpleActivity.createIntent(MainActivity.this, placementIdInput.getEditText().getText().toString()));
                } else {
                    Log.e("KWIZZAD", "not initialized");
                    Snackbar.make(findViewById(R.id.coordinatorLayout), "kwizzad not initialized", Snackbar.LENGTH_INDEFINITE).show();
                }
            });
        }

        View preloadButton = findViewById(R.id.preload);
        if (preloadButton != null) {
            preloadButton.setOnClickListener(v -> {
                if (Kwizzad.isInitialized()) {
                    PreloadingDialogFragment.create(placementIdInput.getEditText().getText().toString())
                            .show(getFragmentManager(), "preload");
                } else {
                    Log.e("KWIZZAD", "not initialized");
                    Snackbar.make(findViewById(R.id.coordinatorLayout), "kwizzad not initialized", Snackbar.LENGTH_INDEFINITE).show();
                }
            });
        }

        View simplePreloadButton = findViewById(R.id.simplePreload);
        if (simplePreloadButton != null) {
            simplePreloadButton.setOnClickListener(v -> {
                if (Kwizzad.isInitialized()) {
                    startActivity(SimplePreloadingExample.createIntent(this, placementIdInput.getEditText().getText().toString()));
                } else {
                    Log.e("KWIZZAD", "not initialized");
                    Snackbar.make(findViewById(R.id.coordinatorLayout), "kwizzad not initialized", Snackbar.LENGTH_INDEFINITE).show();
                }
            });
        }
    }

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
}
