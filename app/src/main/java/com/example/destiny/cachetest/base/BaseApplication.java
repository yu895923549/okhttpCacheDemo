package com.example.destiny.cachetest.base;

import android.app.Application;

/**
 * Created by Destiny on 2016/8/5.
 */

public class BaseApplication extends Application {
    public static BaseApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        this.instance = this;
    }

    public static BaseApplication getInstance() {
        return instance;
    }

}
