package com.kwizzad.api;

public class HttpErrorResponseException extends Exception{
    public final int code;
    public final String message;

    public HttpErrorResponseException(int code, String message) {
        super("http response: "+code+":"+message);
        this.code = code;
        this.message = message;
    }

    /**
     * we already start at code 499 because of nginx sending that for server timeout
     * @return
     */
    public boolean isServerError() {
        return code >=499 && code <=599;
    }
}
