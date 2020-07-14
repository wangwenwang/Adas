package com.minicreate.adas.transmission;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;

import com.minicreate.adas.App;
import com.minicreate.adas.ui.MainActivity;
import com.minicreate.adas.transmission.listener.TcpConnectListener;
import com.minicreate.adas.transmission.listener.TcpRecvListener;
import com.minicreate.adas.transmission.protocol.OnResponseListener;
import com.minicreate.adas.transmission.protocol.Param;
import com.minicreate.adas.transmission.strategy.Strategy;
import com.minicreate.adas.utils.BytesUtil;
import com.minicreate.adas.utils.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WifiEndpoint {
    private static final String TAG = "WifiEndpoint";
    private Context context;
    private String name;
    private Strategy strategy;
    private TcpConnectListener tcpConnectListener;
    private TcpReceiver tcpReceiver;
    private Map<Integer, BusinessFlow.ResponseEntry> businessCacheWaitResp =
            new ConcurrentHashMap<Integer, BusinessFlow.ResponseEntry>();
    private Param beatHeart;
    private byte[] beatHeartData;
    private int beatHeartPeriod = 3000;//3000;//心跳延时，多久发一次心跳
    private static final long TIMEOUT = 30000;//15000;//超时检测，5秒一次

    public  Socket socket;
    private WifiManager mWifiManager;
    private static final String WIFI_HOTSPOT_SSID = "rk1808";
    private static final int    PORT = 10060;
    private InputStream inputStream;
    private OutputStream outputStream;
    private static final int MSG_COMMAND = 1;
    private static final int MSG_TIMEOUT = 2;
    private static final int MSG_HEARTBEAT = 3;
    private ListenerThread mlistenerThread;
    public static final int DEVICE_CONNECTING = 1;//有设备正在连接热点
    public static final int DEVICE_CONNECTED  = 2;//有设备连上热点


    public WifiEndpoint(Context context, String name, Strategy strategy) {
        this.name = name;
        this.context = context;
        this.strategy = strategy;
        beatHeart = strategy.getHeartbeat(context);
        beatHeartData = beatHeart.parseToProtocol();
    }

    public void startEndpoint(TcpConnectListener... tcpConnectListener) {
        this.tcpConnectListener = tcpConnectListener[0];
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        try {
            socket = new Socket(getWifiRouteIPAddress(), PORT);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "Socket connect fail!!!");
        }

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
            tcpReceiver = new TcpReceiver(socket, new TcpRecvListener() {
                @Override
                public void onReceive(byte[] src, int len) {
                    if (src == null) {
                        LogUtil.e(TAG, "src == null");
                        return;
                    }
                    // 反转义
                    src = strategy.unescape(src);
                    // 判断数据是否完整，校验码是否正确
                    boolean isDataCorrect = strategy.checkData(src);
                    if (!isDataCorrect) {
                        //数据不正确
                        LogUtil.e(TAG, "数据不正确");
                        return;
                    }

                    //有些数据是从下位机传上来的，就不需要处理超时了，直接返回
                    boolean continueHandle = strategy.handleMessage(src, context);
                    if (continueHandle) {
                        return;
                    }

                    // 获取协议的认证码，认证码一般是命令字
                    int token = strategy.getToken(src);
                    Message message = handler.obtainMessage(MSG_COMMAND, token, 0, src);
                    handler.sendMessage(message);
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
                    if (tcpReceiver != null) {
                        tcpReceiver.stopRun();
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                            outputStream = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    tcpReceiver = null;
                    WifiEndpoint.this.tcpConnectListener.onConnectError(0, null, 0);
                }

                @Override
                public String getTcpRecvListenerName() {
                    return "WIFIEndpointListener";
                }
            }, strategy);
            tcpReceiver.start();
            this.tcpConnectListener.onConnectOk();
        } else {
            LogUtil.e(TAG, "could not connect");
            this.tcpConnectListener.onConnectError(0, null, 0);
        }

    }

    private  Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            final int token = (msg.arg1) & 0xff;
            LogUtil.d(TAG, "token = " + String.format("%02x", token) + " ,what = " + msg.what);
            final byte[] message = (byte[]) msg.obj;

            switch (msg.what) {
                case MSG_COMMAND:
                    BusinessFlow.ResponseEntry responseEntry = businessCacheWaitResp.get(token);
                    if (responseEntry != null) {
                        OnResponseListener response = responseEntry.onResponseListener;
                        businessCacheWaitResp.remove(token);//删除超时队列
                        if (token != strategy.getToken(responseEntry.param)) {
                            //数据不对，不是我们请求的
                            LogUtil.e(TAG, "不是我们请求的数据，token incorrupt receive token = " + token + " ,send token = " + strategy.getToken(responseEntry.param));
                            break;
                        }
                        Param param = strategy.parseFromProtocol(message, responseEntry.param);
                        //超时设置为false
                        param.setTimeOut(false);
                        LogUtil.d(TAG, "onResponse token = " + String.format("%02x", token) + " ,sumOfKey = " + WifiEndpoint.this.sumOfKey(token));
                        response.onResponse(param);
                    } else {
                        LogUtil.e(TAG, "MSG_COMMAND !!!!!!!!!!!! get(token) null !!!!!!!!!!!! token = " + String.format("%02x", token));
                    }
                    break;

                case MSG_TIMEOUT:
                    BusinessFlow.ResponseEntry responseTimeoutEntry = businessCacheWaitResp.get(token);
                    if (responseTimeoutEntry != null) {
                        final OnResponseListener responseTimeout = responseTimeoutEntry.onResponseListener;
                        businessCacheWaitResp.remove(token);
                        // 超时之后，就发送回原来的BaseParam，BaseParam的超时标记要置为true
                        responseTimeoutEntry.param.setTimeOut(true);
                        responseTimeout.onResponse(responseTimeoutEntry.param);
                        LogUtil.d(TAG, "timeout, remove(token): " + String.format("%02x", token));
                    } else {
                        //没超时
                    }
                    break;
                case MSG_HEARTBEAT: // 5 s后发心跳
                    send(heartbeatResp, beatHeart);
                    LogUtil.d(TAG, "MSG_HEARTBEAT sendBeatHeart");
                    sendHeartbeat(beatHeartPeriod);//延时5秒
                    break;
            }
        }
    };
    private OnResponseListener heartbeatResp =
            new OnResponseListener() {
                @Override
                public void onResponse(Param result) {
                    if (result.isTimeOut()) {
                        LogUtil.d(TAG, "心跳超时 token = " + String.format("%02x", result.getToken()));
                        //心跳没了，要重连
                        tcpConnectListener.onConnectError(0, null, 0);
                    } else {
                        //收到了心跳包，继续发送
                        LogUtil.d(TAG, "heartbeat RET_OK " + String.format("%02x", result.getToken()));
                        //sendHeartbeat(beatHeartPeriod);//延时5秒
                    }
                }
            };
    /**
     * 发送心跳，
     *
     * @param delayed 延时时间，刚连接上USB的时候，此处填0，后续的填5
     */
    public void sendHeartbeat(int delayed) {
        LogUtil.d(TAG, "sendHeartbeat token = " + String.format("%02x", beatHeart.getToken()));
        beatHeartData = beatHeart.parseToProtocol();//每次发送之前需要重新解析一次数据，因为车辆编号会被修改
        Message message = handler.obtainMessage(MSG_HEARTBEAT, strategy.getToken(beatHeart), 0, beatHeartData);
        handler.sendMessageDelayed(message, delayed);
    }

    public void stopEndpoint() {
        LogUtil.d(TAG, "stopEndpoint");
        //将handler中所有的超时检测全部移除
        for (int key : businessCacheWaitResp.keySet()) {
            handler.removeMessages(MSG_TIMEOUT);
        }
        //最后再清空超时列表
        businessCacheWaitResp.clear();

        if(mlistenerThread != null){
            if(mlistenerThread.isAlive()){
                mlistenerThread.interrupt();
                mlistenerThread = null;
            }
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

        if (tcpReceiver != null) {
            tcpReceiver.stopForce();
            tcpReceiver = null;
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

    /**
     * 发送数据
     *
     * @param l
     * @param param
     * @return
     */
    public  int send(OnResponseListener l, Param param) {
        byte[] buf = param.parseToProtocol();
//        d("...send...命令字:" + String.format("%02x", strategy.getToken(param)) + " ,长度:" + buf.length);
        LogUtil.d(TAG,BytesUtil.BytestoHexStringPrintf(buf));
        int result = sendNoRespond(buf);
        //发送数据
        if (outputStream != null) {
            //添加超时队列
            int token = strategy.getToken(buf) & 0xff;
            BusinessFlow.ResponseEntry entry = new BusinessFlow.ResponseEntry(l, param);
            //列出当前超时列表中key为token的数据有多少
            LogUtil.d(TAG, "businessCacheWaitResp.put token = " + String.format("%02x", token) + " ,num = " + sumOfKey(token));
            businessCacheWaitResp.put(token, entry);
            //发送超时检测
            Message message = handler.obtainMessage(MSG_TIMEOUT, token, 0, null);
            handler.sendMessageDelayed(message, TIMEOUT);
        }
        return result;
    }

    /**
     * 发送数据，不需要应答，因此也就不需要处理超时
     *
     * @param param
     * @return
     */
    public  int sendNoRespond(Param param) {
        byte[] buf = param.parseToProtocol();
        LogUtil.d(TAG,"...send...命令字:" + String.format("%02x", strategy.getToken(param)) + " ,长度:" + buf.length);
        return sendNoRespond(buf);
    }

    /**
     * 发送数据，不需要应答，因此也就不需要处理超时
     *
     * @param buf
     * @return 0表示发送成功，-1表示发送失败，失败原因可能是出现异常或者是输出流为null
     */
    public int sendNoRespond(final byte[] buf) {
         int error = 0 ;
        new Thread( new Runnable(){
            @Override
            public void run() {
            //发送数据
                if (outputStream != null) {
                    try {
                        outputStream.write(buf);
                    } catch (IOException e) {
                        LogUtil.e(TAG, "e = " + e.toString()+ e.getClass());
                        e.printStackTrace();
                        // 发送端断开了，需要重连
                        tcpConnectListener.onConnectError(0, null, 0);
                        //error = -1;
                    }
                } else {

                }
            }
        }).start();

        return 0;
    }
    /**
     * 检查超时列表中有多少个key = token的数据
     *
     * @param token
     * @return
     */
    private int sumOfKey(int token) {
        int num = 0;
        for (int a : businessCacheWaitResp.keySet()) {
            if (a == token) {
                num++;
            }
        }
        return num;
    }
    // 打开WIFI
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }
    public WifiConfiguration CreateWifiInfo(String SSID, String Password)
    {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if(tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }


        config.wepKeys[0] = "";
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.wepTxKeyIndex = 0;


        return config;
    }

    private WifiConfiguration IsExsits(String SSID)
    {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs)
        {
            if (existingConfig.SSID.equals("\""+SSID+"\""))
            {
                return existingConfig;
            }
        }
        return null;
    }
    // 添加一个网络并连接
    public void addNetwork(WifiConfiguration wcg) {
        int wcgID = mWifiManager.addNetwork(wcg);
        boolean b =  mWifiManager.enableNetwork(wcgID, true);
        System.out.println("a--" + wcgID);
        System.out.println("b--" + b);
    }
    /**
     * 获取wifi连接信息
     **/
    public WifiInfo getWifiInfo()
    {
        if (mWifiManager != null)
        {
            return mWifiManager.getConnectionInfo();
        }
        return null;
    }
    /**
     * 获取已连接的热点路由
     *
     * @return
     */
    public String getIp() {
        WifiInfo wifiinfo = mWifiManager.getConnectionInfo();
        String localIp = Formatter.formatIpAddress(wifiinfo.getIpAddress());
        Log.i("local Ip ", "wifi local ip：" + localIp);
        return localIp;
    }

    private String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }
    /**
     * wifi获取 已连接网络路由  路由ip地址
     * @param
     * @return
     */
    public String getWifiRouteIPAddress() {
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        //WifiInfo wifiinfo = mWifiManager.getConnectionInfo();
        //        System.out.println("Wifi info----->" + wifiinfo.getIpAddress());
        //        System.out.println("DHCP info gateway----->" + Formatter.formatIpAddress(dhcpInfo.gateway));
        //        System.out.println("DHCP info netmask----->" + Formatter.formatIpAddress(dhcpInfo.netmask));
        //DhcpInfo中的ipAddress是一个int型的变量，通过Formatter将其转化为字符串IP地址
        String routeIp1 = Formatter.formatIpAddress(dhcpInfo.gateway);
        String routeIp2 = Formatter.formatIpAddress(dhcpInfo.serverAddress);
        Log.i("route ip1", "wifi route ip1:" + routeIp1+"   wifi route ip2:" + routeIp2);

        return routeIp2;
    }
    /*
    private Handler handler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_CONNECTING:
                    //connectThread = new ConnectThread(listenerThread.getSocket(), handler);
                    //connectThread.start();
                    break;
                case DEVICE_CONNECTED:
                    //text_state.setText("设备连接成功");
                    break;
                case SEND_MSG_SUCCSEE:
                    //text_state.setText("发送消息成功:" + msg.getData().getString("MSG"));
                    break;
                case SEND_MSG_ERROR:
                    //text_state.setText("发送消息失败:" + msg.getData().getString("MSG"));
                    break;
                case GET_MSG:
                    //text_state.setText("收到消息:" + msg.getData().getString("MSG"));
                    break;
            }
        }
    };*/
    /**
     * 连接线程
     */
    public class ConnectThread extends Thread {

        public final Socket socket;
        private Handler handler;
        public InputStream inputStream;
        public OutputStream outputStream;
        Context context;

        public ConnectThread(Socket socket,Handler handler) {
            setName("ConnectThread");
            Log.i("ConnectThread", "ConnectThread");
            this.socket = socket;
            this.handler = handler;
        }
        @Override
        public void run() {
            Log.d("ConnectThread", "before Enter ConnectThread socket is null!!!");
            if (socket == null) {
                Log.d("ConnectThread", "Enter ConnectThread socket is null!!!");
                LogUtil.e(TAG, "could not connect");

                tcpConnectListener.onConnectError(0, null, 0);
                return;
            }
            //handler.sendEmptyMessage(MainActivity.DEVICE_CONNECTED);
            Log.d("ConnectThread", "after Enter ConnectThread socket is null!!!");
            try {
                //获取数据流
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }
            /*test*/
            try {
                outputStream.write(beatHeartData);

            } catch (IOException e) {
                e.printStackTrace();
            }

            //打开Receiver线程
            tcpReceiver = new TcpReceiver(inputStream, new TcpRecvListener() {
                @Override
                public void onReceive(byte[] src, int len) {
                    if (src == null) {
                        LogUtil.e(TAG, "src == null");
                        return;
                    }
                    // 反转义
                    src = strategy.unescape(src);
                    // 判断数据是否完整，校验码是否正确
                    boolean isDataCorrect = strategy.checkData(src);
                    if (!isDataCorrect) {
                        //数据不正确
                        LogUtil.e(TAG, "数据不正确");
                        return;
                    }

                    //有些数据是从下位机传上来的，就不需要处理超时了，直接返回
                    boolean continueHandle = strategy.handleMessage(src, context);
                    if (continueHandle) {
                        return;
                    }

                    // 获取协议的认证码，认证码一般是命令字
                    int token = strategy.getToken(src);
                    Message message = handler.obtainMessage(MSG_COMMAND, token, 0, src);
                    handler.sendMessage(message);
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
                    if (tcpReceiver != null) {
                        tcpReceiver.stopRun();
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                            outputStream = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    tcpReceiver = null;
                    tcpConnectListener.onConnectError(0, null, 0);
                }

                @Override
                public String getTcpRecvListenerName() {
                    return "USBEndpointListener";
                }
            }, strategy);
            tcpReceiver.start();
            tcpConnectListener.onConnectOk();
        }
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
                    LogUtil.e(TAG, "WifiEndpoint ListenerThread socket is null!!!");
                    ((App)context).setWifiSocketConnectState(false);
                    MainActivity.setWifiSocketConnectState(false);
                    ((App)context).getWifiEndpoint().restartEndpoint();
                }
            }
        }

        public Socket getSocket() {
            return socket;
        }
    }
}

