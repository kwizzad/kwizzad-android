package com.kwizzad.api;

import java.util.List;
import java.util.Map;

public class Response {
    private final Request _request;
    private final int responseCode;
    private final String responseMessage;
    private final String _body;
    private final Map<String, List<String>> _headers;

    public Response(Request request, int responseCode, String responseMessage, Map<String, List<String>> headerFields, String body) {
        this._request = request;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this._body = body;
        this._headers = headerFields;
    }

    public boolean isSuccessful() {
        return responseCode >= 200 && responseCode <= 299;
    }

    public String body() {
        return _body;
    }

    public int code() {
        return responseCode;
    }

    public String message() {
        return responseMessage;
    }

    public Request request() {
        return _request;
    }

    public Map<String, List<String>> headers() {
        return _headers;
    }
}
