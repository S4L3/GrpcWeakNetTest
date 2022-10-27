package com.cloudminds.grpcweaknettest;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.cloudminds.grpcweaknettest.utils.ContextUtils;
import androidx.multidex.MultiDexApplication;

public class WeakNetApplication extends MultiDexApplication {
    private static final String TAG = "WeakNetApplication";
    public static SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        ContextUtils.init(this);
        Log.d(TAG,"onCreate;this" + this);
    }
}
