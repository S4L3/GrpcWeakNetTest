package com.cloudminds.grpcweaknettest.utils;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

public class TimeStatisticsUtils {
    private static final String TAG = "TimeStatisticsUtils";
    private ConcurrentHashMap<Long, Bean> mMap = new ConcurrentHashMap<Long, Bean>(1000);
    private static int mTimesOutputAverage;
    private static int mTimes;
    private static long mAllCostTime;

    private static class SingletonHolder {
        public static TimeStatisticsUtils instance = new TimeStatisticsUtils();
    }

    public static TimeStatisticsUtils getInstance() {
        return TimeStatisticsUtils.SingletonHolder.instance;
    }

    public void init(int timesOutputAverage) {
        mTimesOutputAverage = timesOutputAverage;
        mMap.clear();
        mTimes = 0;
        mAllCostTime = 0;
    }

    public void put(long id, Long sendTime) {
        Bean bean = new Bean();
        bean.Id = id;
        bean.SendTime = sendTime;
        mMap.put(new Long(id), bean);
        Log.d(TAG, "setReceiveTime;id" + id + ";sendTime" + sendTime);
    }

    public void setReceiveTime(long id, long receiveTime) {
        Bean bean = mMap.remove(new Long(id));
        Log.d(TAG, "setReceiveTime;id:" + id + ";receiveTime:" + receiveTime);
        if (bean != null) {
            long costTime = receiveTime - bean.SendTime;
            Log.d(TAG, "Client receive data from server. receiveTime:  " + receiveTime +   "         index:" + id);
            Log.d(TAG, "Client send data to server. sendTime:          " + bean.SendTime + "         index:" + id);
            Log.d(TAG, "Cost Time: " + costTime + " index:" + id);
            mTimes++;
            if (mTimes >= mTimesOutputAverage){
               Log.d(TAG, "Average Cost Time;count:" + mTimes + ";time:" + mAllCostTime/mTimes);
               mAllCostTime = 0;
               mTimes = 0;
            }
            mAllCostTime += costTime;
        }
    }

    public class Bean {
        long Id;
        long SendTime;
        long ReceiveTime;
    }
}