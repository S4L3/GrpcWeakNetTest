package com.cloudminds.grpcweaknettest.control;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

public class GrpcServer {
    private static final String TAG = "GrpcServer";
    private static Server sServer;
    private Boolean mStarted = false;
    private Context mContext;
    private int mPort;

    public GrpcServer(Context context, int port){

    }

    public void start(){
        if (mStarted){
            return;
        }
        Log.d(TAG,"start");
        mStarted = true;
        sServer = NettyServerBuilder.forPort(50051)
                .addService(new WeakNetTestGrpcService())
                .build();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                Log.d(TAG,"addShutdownHook;*** shutting down gRPC server since JVM is shutting down");
                GrpcServer.this.stop();
                Log.d(TAG,"addShutdownHook;*** server shut down");
            }
        });
    }

    public void stop() {
        if (!mStarted){
            return;
        }
        mStarted = false;
        Log.d(TAG,"stop");
        try {
            if (sServer != null) {
                sServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Log.d(TAG,"stop;error:" + e.getMessage());
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (sServer != null) {
            sServer.awaitTermination();
        }
    }
}