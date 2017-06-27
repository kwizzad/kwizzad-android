package com.kwizzad.api;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private final String _method;
    private final byte[] _body;
    private final String _url;
    private final Map<String, String> _headers = new HashMap<>();

    private Request(Builder builder) {
        this._method = builder._method;
        if (builder._body != null && builder._body.content != null) {
            this._body = builder._body.content.getBytes(Charset.forName("UTF-8"));
            this._headers.put("Content-Type", builder._body.mediaType);
        } else {
            this._body = null;
        }
        this._url = builder._url;
    }

    public String method() {
        return _method;
    }

    public String url() {
        return _url;
    }

    public byte[] body() {
        return _body;
    }

    public Map<String, String> headers() {
        return _headers;
    }

    public static class Builder {
        private String _url;
        private String _method;
        private RequestBody _body;

        public Builder method(String httpMethod, RequestBody body) {
            this._method = httpMethod;
            this._body = body;
            return this;
        }

        public Builder url(String value) {
            this._url = value;
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }
}
