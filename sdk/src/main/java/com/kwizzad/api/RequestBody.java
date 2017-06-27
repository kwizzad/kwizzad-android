package com.kwizzad.api;

public class RequestBody {
    public final String mediaType;
    public final String content;

    private RequestBody(String mediaType, String content) {
        this.mediaType = mediaType;
        this.content = content;
    }

    public static RequestBody create(String mediaType, String content) {
        return new RequestBody(mediaType, content);
    }
}
