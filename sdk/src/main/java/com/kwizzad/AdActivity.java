package com.kwizzad;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.kwizzad.log.QLog;

public class AdActivity extends AppCompatActivity {


    private final AdHandler handler = new AdHandler() {
        @Override
        public void close() {
            try {
                finish();
            } catch (Exception ignored) {
            }
        }

        @Override
        public Activity activity() {
            return AdActivity.this;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler.onCreate(getIntent().getExtras(), savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                flags = flags | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }

            getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        setContentView(R.layout.quizdialog);

        if (!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            finish();
            return;
        }

        handler.onCreateView(findViewById(R.id.layout));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        handler.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.onResume();
    }

    @Override
    public void onBackPressed() {
        // NO SUPER. :) we do not want to go back. only explicitly.
        handler.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();

        handler.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.onDestroy();
    }
}
