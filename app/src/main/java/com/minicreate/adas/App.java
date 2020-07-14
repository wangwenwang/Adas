package com.minicreate.adas;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.minicreate.adas.transmission.WifiEndpoint;
import com.minicreate.adas.transmission.listener.TcpConnectListenerAdapter;
import com.minicreate.adas.transmission.strategy.ProtocalAdasStrategy;
import com.minicreate.adas.ui.MainActivity;
import com.minicreate.adas.utils.LogUtil;

public class App extends Application {
    private static final String TAG = "App";
    private WifiEndpoint wifiEndpoint;
    private  boolean isWifiSocketConnected = false;//WIFI SOCKET连接状态
    public WifiThread mwifiThread;

    public static final int WIFISOCKETCONNECT_SUCCESS  = 1;
    public static final int WIFISOCKETCONNECT_FAIL  = 2;
    @Override
    public void onCreate() {
        super.onCreate();

        mwifiThread = new WifiThread();
        //mwifiThread.start();
    }

    public  WifiThread getWifiThread(){
        return mwifiThread;
    }

    /**
     * 初始化WIFI连接端点
     */
    public void initWifiEndpoint(Context context) {
        Log.d(TAG, "App initWifiEndpoint");
        wifiEndpoint = new WifiEndpoint(getApplicationContext(),
                "wifi-endpoint", new ProtocalAdasStrategy());
        wifiEndpoint.startEndpoint(new TcpConnectListenerAdapter() {
            @Override
            public void onConnectError(int code, String server, int port) {
                LogUtil.d(TAG, "App initWifiEndpoint onConnectError");
                isWifiSocketConnected = false;
                Message message = handler.obtainMessage(WIFISOCKETCONNECT_FAIL, 0, 0, null);
                handler.sendMessage(message);

                if(mwifiThread != null){
                    if(mwifiThread.isAlive()){
                        mwifiThread.interrupt();
                        mwifiThread = null;
                    }
                }
                if(wifiEndpoint != null){
                    wifiEndpoint.closeEndpoint();
                    wifiEndpoint = null;
                }

                mwifiThread = new WifiThread();
                mwifiThread.start();
            }

            @Override
            public void onConnectOk() {
                LogUtil.d(TAG, "App initWifiEndpoint onConnectOk");
                isWifiSocketConnected = true;
                Message message = handler.obtainMessage(WIFISOCKETCONNECT_SUCCESS, 0, 0, null);
                handler.sendMessage(message);

                if(wifiEndpoint != null){
                    //连接成功之后，发送心跳，延时为0秒
                    wifiEndpoint.sendHeartbeat(0);
                }
            }
        });
    }

    public WifiEndpoint getWifiEndpoint() {
        return wifiEndpoint;
    }
    public  void setWifiSocketConnectState(boolean wifisocketconnectstate){
        isWifiSocketConnected = wifisocketconnectstate;
    }
    public boolean isWifiSocketConnected() {
        return isWifiSocketConnected;
    }

    public class WifiThread extends Thread {
        @Override
        public void run() {
            initWifiEndpoint(getApplicationContext());
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WIFISOCKETCONNECT_SUCCESS:
                case WIFISOCKETCONNECT_FAIL:
                    MainActivity.setWifiSocketConnectState(isWifiSocketConnected);
                    break;
            }
        }
    };
}
