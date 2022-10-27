package com.cloudminds.grpcweaknettest.control;

import android.util.Log;

import io.grpc.stub.StreamObserver;
import weaknettest.WeakNetTestGrpc;
import weaknettest.Weaknettest;

public class WeakNetTestGrpcService extends WeakNetTestGrpc.WeakNetTestImplBase {
    private final static String TAG = "WeakNetTestGrpcService";

    public WeakNetTestGrpcService() {
    }

    @Override
    public StreamObserver<Weaknettest.Request> pressureTest(final StreamObserver<Weaknettest.Response> responseObserver) {
        return new StreamObserver<Weaknettest.Request>() {
            @Override
            public void onNext(Weaknettest.Request note) {
                Log.d(TAG, "onNext");
                Weaknettest.Response response = Weaknettest.Response.newBuilder()
                        .setId(note.getId())
                        .setData(note.getData())
                        .build();
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                Log.d(TAG, "onError;" + t.getMessage());
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "onCompleted");
                responseObserver.onCompleted();
            }
        };
    }
}
