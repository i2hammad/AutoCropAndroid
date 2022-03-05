package com.i2hammad.autocropandroid;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SmartCropper.buildImageDetector(this);
    }
}
