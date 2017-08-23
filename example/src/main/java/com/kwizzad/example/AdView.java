package com.kwizzad.example;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.kwizzad.AbstractPlacementModel;
import com.kwizzad.model.ImageInfo;
import com.kwizzad.model.events.Reward;

import java.net.URL;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by tvsmiles on 12.05.17.
 */

public class AdView extends FrameLayout {
    private ImageView ivKwizzImage;
    private TextView tvTeaser;
    private TextView tvPointsAmount;
    private CardView ad;
    private TextView tvAdTitle;

    public AdView(Context context) {
        super(context);
        init(context);
    }

    public AdView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.ad_view, this, true);
        ivKwizzImage = (ImageView) findViewById(R.id.iv_kwizz_image);
        tvTeaser = (TextView) findViewById(R.id.tv_teaser);
        tvPointsAmount = (TextView) findViewById(R.id.tv_points);
        tvAdTitle = (TextView) findViewById(R.id.tv_ad_title);
        ad = (CardView) findViewById(R.id.ad);
    }

    public void setLoading() {
        ad.setEnabled(false);
        tvTeaser.setText(getContext().getString(R.string.loading_text));
        tvAdTitle.setText(getContext().getString(R.string.loading_text));
        tvPointsAmount.setText(getContext().getString(R.string.loading_text));
        ivKwizzImage.setImageResource(R.drawable.kwizzad_logo);
    }

    public void setDismissed() {
        ad.setEnabled(false);
        tvTeaser.setText(getContext().getString(R.string.dismissed_text));
        tvAdTitle.setText(getContext().getString(R.string.dismissed_text));
        tvPointsAmount.setText(getContext().getString(R.string.dismissed_text));
        ivKwizzImage.setImageResource(R.drawable.kwizzad_logo);
    }

    public void setPlacementModel(AbstractPlacementModel placementModel) {
        ad.setEnabled(true);
        Iterable<Reward> rewards = placementModel.getRewards();
        Iterable<Reward> summarizedRewards = Reward.summarize(rewards);

        Observable.fromCallable(() -> {
                    URL newurl = new URL(placementModel.getAdResponse().squaredThumbnailURL());
                    return BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmap -> {
                            if (ivKwizzImage != null) {
                                ivKwizzImage.setImageBitmap(bitmap);
                            }
                        },
                        throwable -> {
                            if (ivKwizzImage != null) {
                                ivKwizzImage.setImageResource(R.drawable.kwizzad_logo);
                            }
                        });

        tvTeaser.setText(placementModel.getTeaser());
        tvAdTitle.setText(placementModel.getHeadline());

        if(summarizedRewards.iterator().hasNext()) {
            if(summarizedRewards.iterator().next().amount > 0) {
                tvPointsAmount.setText("+" + summarizedRewards.iterator().next().amount);
                tvPointsAmount.setVisibility(VISIBLE);
            } else {
                tvPointsAmount.setVisibility(GONE);
            }
        } else {
            int totalReward = 0;
            for (Reward reward: rewards) totalReward += reward.amount;

            if(totalReward > 0) {
                tvPointsAmount.setText("+" + totalReward);
                tvPointsAmount.setVisibility(VISIBLE);
            } else {
                tvPointsAmount.setVisibility(GONE);
            }
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        ad.setOnClickListener(l);
    }
}
