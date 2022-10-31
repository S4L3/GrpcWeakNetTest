package com.cloudminds.grpcweaknettest.control;

import android.content.Context;
import android.util.Log;

import com.cloudminds.grpcweaknettest.utils.TimeStatisticsUtils;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.cronet.CronetChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;
import weaknettest.WeakNetTestGrpc;
import weaknettest.Weaknettest;
import org.chromium.net.ExperimentalCronetEngine;

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
                sEngine = new ExperimentalCronetEngine.Builder(context /* Android Context */).
                        enableHttp2(false).
                        enableQuic(true).
                        build();
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