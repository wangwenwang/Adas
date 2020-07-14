package com.minicreate.adas.transmission;


import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Message;

import com.minicreate.adas.App;
import com.minicreate.adas.transmission.listener.TcpConnectListener;
import com.minicreate.adas.transmission.listener.TcpRecvListener;
import com.minicreate.adas.ui.MainActivity;
import com.minicreate.adas.utils.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class TcpVideoEndpoint implements TcpConnectListener {
    private static final String TAG = "TcpVideoEndpoint";
    private Context context;
    private TcpConnectListener tcpConnectListener;

    public static final int    PORT = 10061;
    private  Socket socket;
    private ListenerThread mlistenerThread;
    private InputStream inputStream;
    private OutputStream outputStream;
    private TcpVideoReceiver tcpVideoReceiver;
    private TcpConnector connector;

    public TcpVideoEndpoint(Context context) {

        this.context = context;
    }
    public void startEndpoint(TcpConnectListener... tcpConnectListener) {
        this.tcpConnectListener = tcpConnectListener[0];
        //mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        connector = new TcpConnector(PORT,this);
        connector.start();

    }
    @Override
    public void onConnectStart(String server, int port) {
        LogUtil.d(TAG,"onConnectStart server = " + server + " ,port = " + port);
    }

    @Override
    public void onConnectOk() {

    }
    @Override
    public void onConnectOk(Socket clientSocket, String server, int port) {
        this.socket = clientSocket;
        LogUtil.d(TAG,"onConnectOk socket = " + socket);
        if (socket != null) {
            mlistenerThread = new ListenerThread(socket);
            mlistenerThread.start();

            try {
                //获取数据流
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }
            //打开Receiver线程
            tcpVideoReceiver = new TcpVideoReceiver(inputStream, new TcpRecvListener() {
                @Override
                public void onReceive(byte[] src, int len) {

                }

                @Override
                public void onReceiveBegin() {

                }

                @Override
                public void onReceiveEnd() {

                }

                @Override
                public void onRecvNetException() {
                    LogUtil.e(TAG, "onRecvNetException");
                    // 对方断开了连接，需要重连
                    if (tcpVideoReceiver != null) {
                        tcpVideoReceiver.stopRun();
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                            outputStream = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    tcpVideoReceiver = null;
                    TcpVideoEndpoint.this.tcpConnectListener.onConnectError(0, null, 0);
                }

                @Override
                public String getTcpRecvListenerName() {
                    return "WIFIEndpointListener";
                }
            });
            tcpVideoReceiver.start();
            this.tcpConnectListener.onConnectOk();
        } else {
            LogUtil.e(TAG, "could not connect");
            this.tcpConnectListener.onConnectError(0, null, 0);
        }
    }
    @Override
    public void onConnectError(int code, String server, int port){

    }
    public void stopEndpoint() {
        LogUtil.d(TAG, "stopEndpoint");

        if(mlistenerThread != null){
            if(mlistenerThread.isAlive()){
                mlistenerThread.interrupt();
                mlistenerThread = null;
            }
        }
        if (connector != null) {
            connector.stopRun();
            connector = null;
        }
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
                outputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (tcpVideoReceiver != null) {
            tcpVideoReceiver.stopForce();
            tcpVideoReceiver = null;
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
        } finally {
            socket = null;
        }

    }

    public void restartEndpoint() {
        LogUtil.d(TAG, "restartEndpoint currentThread = " + Thread.currentThread().toString());
        stopEndpoint();
        startEndpoint(tcpConnectListener);
    }

    public void closeEndpoint() {
        LogUtil.d(TAG, "closeEndpoint");
        stopEndpoint();
    }

    public class ListenerThread extends Thread {
        private Socket  socket;

        public ListenerThread(Socket  socket) {
            setName("ListenerThread");
            this.socket = socket;
        }

        @Override
        public void run() {
            while (true) {
                if(socket == null)
                {
                    LogUtil.e(TAG, "TcpVideoEndpoint ListenerThread socket is null!!!");
                    restartEndpoint();
                }
            }
        }

        public Socket getSocket() {
            return socket;
        }
    }
}