package com.minicreate.adas.transmission;

import com.minicreate.adas.transmission.listener.TcpRecvListener;
import com.minicreate.adas.transmission.strategy.ProtocalAdasStrategy;
import com.minicreate.adas.transmission.strategy.Strategy;
import com.minicreate.adas.utils.BytesUtil;
import com.minicreate.adas.utils.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Clock
 * @version V1.0
 * @ClassName: TcpReceiver.java
 * @Date 2014-9-19 上午11:54:33
 * @Description: TCP端点 接收线程
 */
public class TcpReceiver extends Thread {
    private InputStream in;
    private Boolean isForceStop = false;
    private final String TAG = "TcpReceiver";
    private TcpRecvListener listeners;
    private Strategy strategy;

    public TcpReceiver(InputStream in, TcpRecvListener tdl, Strategy strategy) {
        this.in = in;
        listeners = tdl;
        this.strategy = strategy;
    }

    public TcpReceiver(Socket socket, TcpRecvListener tdl, Strategy strategy) {
        try {
            this.strategy = strategy;
            in = socket.getInputStream();
            listeners = tdl;
            isForceStop = false;
            d("TcpReceiver 构造函数。。。。。。。。。。。。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void notifyDataReceive(byte[] buf, int len) {
        listeners.onReceive(buf, len);
    }

    @Override
    public void run() {
        d("TcpReceiver run...");
        int errorCount = 0;
        byte[] recvArray;
        recvArray = new byte[1024];
        List<byte[]> packageList;

        while (!isInterrupted()) {
            try {
                int length = 0;
                length = in.read(recvArray);//流的末尾会返回-1, 像你这种情况就是当对方将socket的输出流关闭后， 你将对方的输出都读完后，再读下一个字节就会返回-1.
                LogUtil.v(TAG, "本次包，接收到的长度：" + length);
                if (length > 0) {
                    byte[] src = new byte[length];
                    System.arraycopy(recvArray, 0, src, 0, length);
                    //发布的时候最好不要直接输出原始数据，会很卡，调试的时候输出就行
                   LogUtil.d(TAG, "length = " + length + " , 原始数据 src = " + BytesUtil.BytestoHexStringPrintf(src));
                    List<byte[]> resultList = strategy.findPackage(src);
                    if (resultList != null) {
//                        LogUtil.d(TAG, "resultList.size = " + resultList.size());
                        for (byte[] tmp : resultList) {
//                            LogUtil.d(TAG, "receive token = " + String.format("%02x", strategy.getToken(strategy.unescape(tmp)) & 0xff) + " ,data = " + BytesUtil.BytestoHexStringPrintf(tmp));
                            notifyDataReceive(tmp, tmp.length);
                        }
                    } else {
                        LogUtil.d(TAG, "resultList is null");
                    }
                } else if (length < 0) {
//                    errorCount++;
//                    d("接收长度为负，接收到10次负数会关闭接收线程，当前第" + errorCount + "次");
//                    if (errorCount > 10) {
//                        break;
//                    }
                } else {
                    //接收到的长度是0
                }
            } catch (SocketException e) {
                break;
            } catch (IOException e) {
                break;
            }
        }
        stopException();
    }

    public void stopException() {
        if (!isForceStop) {
            new Timer().schedule(new TimerTask() {

                @Override
                public void run() {
                    listeners.onRecvNetException();
                }
            }, 1000);
        }
    }

    public void stopForce() {
        isForceStop = true;
        stopRun();
    }

    public void stopRun() {
        interrupt();
        close();
    }

    private void close() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void d(String msg) {
        LogUtil.w(TAG, msg);
    }
}
