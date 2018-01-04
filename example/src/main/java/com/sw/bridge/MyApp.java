package com.sw.bridge;

import android.app.Application;


public class MyApp extends Application {

    public static Application application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }
}
