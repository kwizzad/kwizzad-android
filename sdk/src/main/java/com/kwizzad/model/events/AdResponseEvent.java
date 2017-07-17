package com.kwizzad.model.events;

import com.kwizzad.Util;
import com.kwizzad.db.FromJson;
import com.kwizzad.model.ImageInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdResponseEvent extends AAdEvent {

    public EAdType adType;
    public Date expiry;
    public String url;
    public String goalUrlPattern;
    private List<Reward> rewardList = new ArrayList<>();
    private List<ImageInfo> imageUrls = new ArrayList<>();
    private String headLine;
    private String teaser;
    private String brand;

    private int estimatedTimeForPlayingACampaign = 20 ;// in seconds

    public CloseType closeButtonVisibility;

    public enum CloseType {
        OVERALL("OVERALL"),
        BEFORE_CALL2ACTION("BEFORE_CALL2ACTION"),
        AFTER_CALL2ACTION("AFTER_CALL2ACTION"),
        AFTER_CALL2ACTION_PLUS("AFTER_CALL2ACTION_PLUS");

        public final String key;

        CloseType(String key) {
            this.key = key;
        }

        public static CloseType fromKey(String key) {
            if (key == null)
                return OVERALL;

            for (CloseType type : values()) {
                if (type.key.equalsIgnoreCase(key)) {
                    return type;
                }
            }
            return OVERALL;
        }
    }

    @FromJson
    @Override
    public void from(JSONObject o) throws JSONException {
        super.from(o);
        adType = EAdType.fromKey(o.getString("adType"));
        expiry = Util.fromISO8601(o.getString("expiry"));
        url = o.getString("url");
        closeButtonVisibility = CloseType.fromKey(o.optString("closeButtonVisibility"));


        goalUrlPattern = o.optString("goalUrlPattern", null);
        if (goalUrlPattern != null && goalUrlPattern.trim().length() == 0)
            goalUrlPattern = null;

        rewardList.clear();
        if (o.has("rewards")) {
            rewardList = Reward.fromArray(o.getJSONArray("rewards"));
        }

        imageUrls.clear();
        if(o.has("images")) {
            JSONArray jsonArrayImages = o.getJSONArray("images");
            for (int i = 0; i < jsonArrayImages.length(); i++) {
                JSONObject imageObject = new JSONObject(jsonArrayImages.getString(i));
                imageUrls.add(new ImageInfo(imageObject.optString("urlTemplate"), imageObject.optString("type")));
            }
        }

        JSONObject ad = new JSONObject(o.getString("ad"));
        headLine = ad.getString("headline");
        teaser = ad.getString("teaser");
        brand = ad.getString("brand");
    }

    public Iterable<Reward> rewards() {
        return rewardList;
    }

    public List<ImageInfo> getImageUrls() {
        return imageUrls;
    }

    public String getHeadLine() {
        return headLine;
    }

    public String getTeaser() {
        return teaser;
    }

    public String getBrand() {
        return brand;
    }

    public Boolean adWillExpireSoon(){
        if (expiry != null) {
            return expiry.getTime() - estimatedTimeForPlayingACampaign * 1000 < 0;
        }
        return false;
    }

    public Long timeToExpireMillis() {
        if (expiry != null) {
            return expiry.getTime() - System.currentTimeMillis() - estimatedTimeForPlayingACampaign * 1000;
        }
        return null;
    }

    public String squaredThumbnailURL(int width) {
        for(ImageInfo info : imageUrls) {
            if (info.getType().equalsIgnoreCase("header")) {
                return info.getUrl(width, width);
            }
        }
        return null;
    }

    public String squaredThumbnailURL() {
        return squaredThumbnailURL(200);
    }

}
