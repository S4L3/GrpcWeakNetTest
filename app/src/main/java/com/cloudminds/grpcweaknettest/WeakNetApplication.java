package com.cloudminds.grpcweaknettest;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.cloudminds.grpcweaknettest.utils.ContextUtils;
import androidx.multidex.MultiDexApplication;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

public class WeakNetApplication extends MultiDexApplication {
    private static final String TAG = "WeakNetApplication";
    public static SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        ContextUtils.init(this);
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        Log.d(TAG,"onCreate;this" + this);
    }
}
