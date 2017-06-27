package com.kwizzad.model;


public interface IUserDataModel {
    void setUserId(String id);

    String getUserId();

    void setGender(Gender gender);

    Gender getGender();

    void setFacebookUserId(String id);

    String getFacebookUserId();

    void setName(String name);

    String getName();

}
