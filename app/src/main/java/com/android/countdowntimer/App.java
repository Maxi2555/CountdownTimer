package com.android.countdowntimer;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

import io.paperdb.Paper;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        Paper.init(this);
    }
}
