package com.cloudminds.grpcweaknettest.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.cloudminds.grpcweaknettest.control.GrpcClient;
import com.cloudminds.grpcweaknettest.control.GrpcServer;
import com.cloudminds.grpcweaknettest.R;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";
    private GrpcClient mClient = null;
    private GrpcServer mServer = null;
    private EditText mClientHost = null;
    private EditText mClientPort = null;
    private Spinner mClientSpinner = null;

    private EditText mServerPort = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"MainActivity;onCreate");

        Button startClientButton = findViewById(R.id.Button_Start_Client);
        startClientButton.setOnClickListener((View v)->{startClient(true);});

        Button stopClientButton = findViewById(R.id.Button_Stop_Client);
        startClientButton.setOnClickListener((View v)->{startClient(false);});

        Button startServerButton = findViewById(R.id.Button_Start_Server);
        startServerButton.setOnClickListener((View v)->{startServer(true);});

        Button stopServerButton = findViewById(R.id.Button_Stop_Server);
        startClientButton.setOnClickListener((View v)->{startServer(false);});

        mClientHost = findViewById(R.id.Edit_Text_Host);
        mClientPort = findViewById(R.id.Edit_Text_Port);
        mClientSpinner = findViewById(R.id.Spinner_Channel_Type);

        mServerPort = findViewById(R.id.Edit_Text_Server_Port);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            mClient.stop();
        }
        if (mServer != null) {
            mServer.stop();
        }
    }

    public void startClient(boolean start) {
        Log.d(TAG,"startClient;" + (start ? "true" : "false"));
        if (!start){
            if (mClient != null) {
                mClient.stop();
                mClient = null;
            }
            return;
        }
        if(mClient != null){
            Log.d(TAG,"startClient;already start");
            return;
        }
        String hostStr = mClientHost.getText().toString();
        String host = TextUtils.isEmpty(hostStr) ? "10.13.72.143" : hostStr;

        String portStr = mClientPort.getText().toString();
        int port = TextUtils.isEmpty(portStr) ? 50051 : Integer.valueOf(portStr);

        String channelTypeStr = String.valueOf(mClientSpinner.getSelectedItem());
        int channelType = mClientSpinner.getSelectedItemPosition();
        Log.d(TAG,"startClient;host:" + host + ";portStr:" + portStr +
                ";channelTypeStr:" + channelTypeStr +
                ";channelType:"+channelType);

        mClient = new GrpcClient(this, host, port, channelType);
        mClient.start();
    }

    public void startServer(boolean start){
        Log.d(TAG,"startServer;" + (start ? "true" : "false"));
        if (!start){
            if (mServer != null) {
                mServer.stop();
                mServer = null;
            }
            return;
        }
        if(mServer != null){
            Log.d(TAG,"startServer;already start");
            return;
        }
        String portStr = mClientPort.getText().toString();
        int port = TextUtils.isEmpty(portStr) ? 50051 : Integer.valueOf(portStr);

        mServer = new GrpcServer(this, port);
        mServer.start();
    }
}