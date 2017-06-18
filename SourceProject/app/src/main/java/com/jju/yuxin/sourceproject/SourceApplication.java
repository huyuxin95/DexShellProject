package com.jju.yuxin.sourceproject;

import android.app.Application;
import android.util.Log;

/**
 * =============================================================================
 * Copyright (c) 2017 yuxin All rights reserved.
 * Packname com.jju.yuxin.sourceproject
 * Created by yuxin.
 * Created time 2017/6/18 0018 下午 4:03.
 * Version   1.0;
 * Describe :
 * History:
 * ==============================================================================
 */

public class SourceApplication extends Application {
    private static final String TAG=SourceApplication.class.getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"-------------onCreate");
    }
}
