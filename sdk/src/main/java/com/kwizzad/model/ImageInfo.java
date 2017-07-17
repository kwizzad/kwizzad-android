package com.kwizzad.model;

/**
 * Created by tvsmiles on 14.07.17.
 */

public class ImageInfo {
    private String url = null;
    private String type = null;

    public ImageInfo(String url, String type) {
        this.url = url;
        this.type = type;
    }

    public String getRawUrl() {
        return url;
    }

    public String getUrl(int width, int height) {
        return url.replace("{{width}}", Integer.toString(width)).replace("{{height}}", Integer.toString(height));
    }

    public String getType() {
        return type;
    }
}
