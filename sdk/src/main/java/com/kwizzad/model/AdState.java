package com.kwizzad.model;

public enum AdState {
    /**
     * -> REQUESTING_AD
     */
    INITIAL,

    /**
     * We are currently requesting an ad.
     * <p>
     * -> RECEIVED_AD, NOFILL
     */
    REQUESTING_AD,

    /**
     * successful ad request, but no ad was available to be returned.
     */
    NOFILL,

    /**
     * -> LOADING_AD
     */
    RECEIVED_AD,

    /**
     * -> AD_READY
     */
    LOADING_AD,

    /**
     * Ad was loaded successfully and is ready to be shown.
     * <p>
     * ->  SHOWING_AD, DISMISSED
     */
    AD_READY,

    /**
     * -> CALL2ACTION, DISMISSED
     */
    SHOWING_AD,

    /**
     * -> GOAL_REACHED, DISMISSED
     */
    CALL2ACTION,

    /**
     * TODO: fix documentation
     */
    CALL2ACTIONCLICKED,

    /**
     * shows OK button instead of X currently
     * <p>
     * -> DISMISSED
     */
    GOAL_REACHED,

    /**
     * Ad was closed.
     */
    DISMISSED,
}