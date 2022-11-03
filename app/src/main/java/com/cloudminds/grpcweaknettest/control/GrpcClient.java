package com.cloudminds.grpcweaknettest.control;

import android.content.Context;
import android.util.Log;

import com.cloudminds.grpcweaknettest.utils.CertUtils;
import com.cloudminds.grpcweaknettest.utils.TimeStatisticsUtils;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.cronet.CronetChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;
import weaknettest.WeakNetTestGrpc;
import weaknettest.Weaknettest;

import org.chromium.net.CronetEngine;
import org.chromium.net.ExperimentalCronetEngine;
import org.json.JSONException;
import org.json.JSONObject;

public class GrpcClient {
    private static final String TAG = "GrpcClient";
    private static long sDataSize = 40 * 1024; //单位byte
    private static long sSendInterval = 30;   //单位毫秒
    private static int sSendMaxTimes = 100;
    private Thread mSendThread;
    private Boolean mStarted = false;
    private byte[] mSendByteData = new byte[sSendMaxTimes];
    private Context mContext;
    private static int sId = 0;

    public GrpcClient(Context context, String host, int port, int channelType) {
        init(context, host, port, channelType);
    }

    public GrpcClient(Context context, long size, long interval, int maxTimes, String host, int port, int channelType) {
        sDataSize = size;
        if (interval <= 0) {
            throw new IllegalArgumentException("interval:" + interval);
        }
        sSendInterval = interval;
        sSendMaxTimes = maxTimes;
        init(context, host, port, channelType);
    }

    private void init(Context context, String host, int port, int channelType) {
        mSendByteData = new byte[sSendMaxTimes];
        mContext = context;
        GrpcChannelUtils.initChannel(context, host, port, channelType);
    }

    public synchronized void start() {
        if (mStarted) {
            Log.d(TAG, "start;error already start");
            return;
        }
        mStarted = true;
        Log.d(TAG, "start");
        TimeStatisticsUtils.getInstance().init(sSendMaxTimes);

        //int port = TextUtils.isEmpty(portStr) ? 0 : Integer.valueOf(portStr);
        //WeakNetTestGrpc.WeakNetTestBlockingStub blockingStub = WeakNetTestGrpc.newBlockingStub(channel);
        WeakNetTestGrpc.WeakNetTestStub asyncStub = WeakNetTestGrpc.newStub(GrpcChannelUtils.getChannel());

        StreamObserver<Weaknettest.Request> requestObserver =
                asyncStub.pressureTest(
                        new StreamObserver<Weaknettest.Response>() {
                            @Override
                            public void onNext(Weaknettest.Response note) {
                                TimeStatisticsUtils.getInstance().setReceiveTime(note.getId(), System.currentTimeMillis());
                                Log.d(TAG, "onNext");
                            }

                            @Override
                            public void onError(Throwable t) {
                                Log.d(TAG, "onError;Throwable:" + t);
                                Log.d(TAG, "onError;Throwable:" + android.util.Log.getStackTraceString(t));
                            }

                            @Override
                            public void onCompleted() {
                                Log.d(TAG, "onCompleted");
                            }
                        });
        mSendThread = new Thread(() -> {
            while (mStarted) {
                try {
                    Thread.sleep(sSendInterval);

                    long sendTime = System.currentTimeMillis();
                    TimeStatisticsUtils.getInstance().put(sId, sendTime);
                    Weaknettest.Request requestData = Weaknettest.Request.newBuilder()
                            .setId(sId)
                            .setData(com.google.protobuf.ByteString.copyFrom(mSendByteData))
                            .build();

                    requestObserver.onNext(requestData);
                    sId++;
                } catch (InterruptedException e) {
                    Log.d(TAG, "mSendThread;error:InterruptedException");
                } catch (Exception e) {
                    Log.d(TAG, "mSendThread;error:" + e.getMessage());
                }
            }
            Log.d(TAG, "mSendThread;exit");
        });
        mSendThread.start();
    }

    public synchronized void stop() {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        GrpcChannelUtils.getChannel().shutdown();
    }

    public static class GrpcChannelUtils {
        private static final String TAG = "GrpcUtils";

        public static final int CHANNEL_TYPE_OKHTTP = 0;
        public static final int CHANNEL_TYPE_NETTY = 1;
        public static final int CHANNEL_TYPE_CRONET = 2;

        private static ExperimentalCronetEngine sEngine;
        private static ManagedChannel sChannel;
        private static String mHost;
        private static int mType;
        private static int mPort;

        public static void initChannel(Context context, String host, int port, int type) {
            mHost = host;
            mPort = port;
            mType = type;
            sChannel = null;
            if (CHANNEL_TYPE_OKHTTP == mType) {
                sChannel = OkHttpChannelBuilder.forAddress(mHost, mPort)
                        .keepAliveTime(1, TimeUnit.MINUTES)
                        .keepAliveTimeout(5, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .usePlaintext()
                        .build();
            } else if (CHANNEL_TYPE_NETTY == mType) {
                sChannel = NettyChannelBuilder.forAddress(mHost, mPort)
                        .keepAliveTime(1, TimeUnit.MINUTES)
                        .keepAliveTimeout(5, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .usePlaintext()
                        .build();
            } else if (CHANNEL_TYPE_CRONET == mType) {
                JSONObject json = new JSONObject();
                try {
                    json.put("InsecureSkipVerify", true);
                } catch (Exception e) {
                    Log.e(TAG, "CHANNEL_TYPE_CRONET;error:" + e.getMessage());
                }

                Set<byte[]> pinsSha256 = CertUtils.getPinsSha256();
                int index = 0;
                for (byte[] data : pinsSha256) {
                    for (int in_index = 0; in_index < data.length; in_index++) {
                        Log.i(TAG, "pinsSha256[" + index + "]-[" + in_index + "]:" + data[in_index]);
                    }
                    index ++;
                }

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.SECOND, Integer.MAX_VALUE);
                sEngine = new ExperimentalCronetEngine.Builder(context /* Android Context */)
                        .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY,5*1024 * 1024)
                        .enableHttp2(true)
                        .enableQuic(true)
                        .enableSdch(true)
                        .addPublicKeyPins("www.test1111.com", pinsSha256, false, cal.getTime())
                        .addQuicHint("10.11.33.78", 50051, 50051)
                        .addQuicHint(mHost, mPort, mPort)
                        .build();

                sChannel = CronetChannelBuilder.forAddress(mHost, mPort, sEngine)
                        //.keepAliveTime(1, TimeUnit.MINUTES)
                        //.keepAliveTimeout(5, TimeUnit.SECONDS)
                        //.keepAliveWithoutCalls(true)
                        //.usePlaintext()
                        .build();
            } else {
                throw  new IllegalArgumentException("channel type:"+ type + "not support");
            }
        }

        public static ManagedChannel getChannel() {
            return sChannel;
        }
    }
}