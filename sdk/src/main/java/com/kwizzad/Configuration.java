package com.kwizzad;

import android.content.Context;

public final class Configuration {

    public final Context context;
    public final String apiKey;
    public final String overrideServer;
    public final boolean debug;
    public final String overrideWeb;

    private Configuration(Builder builder) {
        this.context = builder._applicationContext;
        this.apiKey = builder._apiKey;
        this.overrideServer = builder._overrideServer;
        this.debug = builder._debug;
        this.overrideWeb = builder._overrideWeb;
    }

    public static final class Builder {

        private Context _applicationContext;
        private String _apiKey;
        private String _overrideServer;
        private boolean _debug;
        private String _overrideWeb;

        public Configuration build() {
            return new Configuration(this);
        }

        public Builder applicationContext(Context value) {
            this._applicationContext = value.getApplicationContext();
            return this;
        }

        public Builder apiKey(String value) {
            this._apiKey = value;
            return this;
        }

        public Builder debug(boolean value) {
            this._debug = value;
            return this;
        }

        public Builder overrideServer(String server) {
            this._overrideServer = server;
            return this;
        }

        public Builder overrideWeb(String url) {
            this._overrideWeb = url;
            return this;
        }
    }
}
