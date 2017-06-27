package com.kwizzad;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kwizzad.log.QLog;

public class AdSupportDialogFragment extends DialogFragment {

    private final AdHandler handler = new AdHandler() {
        @Override
        public void close() {
            try {
                dismiss();
            } catch (Exception ignored) {
            }
        }

        @Override
        public Activity activity() {
            return getActivity();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.QuizDialogTheme);

        if(!Kwizzad.isInitialized()) {
            QLog.d("sdk has not completed initialization yet. can not request ad yet. please wait until it finishes.");
            dismiss();
            return;
        }

        handler.onCreate(getArguments(), savedInstanceState);

    }

    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && getDialog() != null) {

            //getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

            QLog.d("hide system ui");
            final View decorView = getDialog().getWindow().getDecorView();

            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                handler.onBackPressed();
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.quizdialog, container, false);

        handler.onCreateView(view);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.onResume();

        hideSystemUI();

    }

    @Override
    public void onPause() {
        super.onPause();
        handler.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.onDestroy();
    }
}
