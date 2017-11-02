package com.kwizzad.example;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.kwizzad.Configuration;
import com.kwizzad.Kwizzad;
import com.kwizzad.log.QLog;
import com.kwizzad.model.Gender;

import java.util.UUID;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Kwizzad.init(
                new Configuration.Builder()
                        .applicationContext(this)
                        .apiKey("b81e71a86cf1314d249791138d642e6c4bd08240f21dd31811dc873df5d7469d")
                        .debug(true)

                        //
                        
                        //.
                        //

                        .build()
        );

        QLog.d("sdk finished initializing");

        QLog.i("userId "+Kwizzad.getUserData().getUserId());
        QLog.i("gender "+Kwizzad.getUserData().getGender());
        QLog.i("username "+Kwizzad.getUserData().getName());

        Kwizzad.getUserData().setUserId(getUserId());
        Kwizzad.getUserData().setGender(Gender.MALE);
        Kwizzad.getUserData().setName("Horst Günther");
    }

    /**
     * Obtain user id, you can set here whatever id you want
     * @return
     */
    private String getUserId() {
        SharedPreferences sp = getSharedPreferences("userdata", Context.MODE_PRIVATE);
        String userId = sp.getString("userId", "");
        if(userId.isEmpty()) {
            userId = UUID.randomUUID().toString();
            sp.edit()
                    .putString("userId", userId)
                    .apply();
        }

        return userId;
    }
}
