package com.minicreate.adas.transmission;

import android.util.Log;

import com.minicreate.adas.control.VideoDataHandler;
import com.minicreate.adas.transmission.listener.TcpRecvListener;

import com.minicreate.adas.utils.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class TcpVideoReceiver extends Thread {
    private InputStream in;
    private Boolean isForceStop = false;
    private final String TAG = "TcpVideoReceiver";

    private TcpRecvListener listeners;
    private Socket socket;

    public TcpVideoReceiver(Socket socket, TcpRecvListener tdl) {
        try {
            socket.setSoTimeout(0);
            this.socket = socket;
            in = socket.getInputStream();
            listeners = tdl;
            isForceStop = false;
            d("TcpVideoReceiver construction method");
        } catch (IOException e) {
            printException(e);
        }
    }
    public TcpVideoReceiver(InputStream in, TcpRecvListener tdl) {
        this.in = in;
        listeners = tdl;
    }
    void notifyDataReceive(byte[] buf, int len) {
//		listeners.onReceive(buf, len);
        VideoDataHandler.getInstence().pushData(buf, len);
    }

    private int errorCount = 0;

    @Override
    public void run() {

        d("TcpVideoReceiver run..."
                + Thread.currentThread().getName());
        final int size = 102400;
        byte[] result = new byte[size];
        while (!isInterrupted()) {
            try {
                int length = in.read(result, 0, size);
                if (length > 0) {
                    notifyDataReceive(result, length);

                } else if (length < 0)// error happened when tcp is // interrupted by phone call
                { // 服务器关掉 也这样
                    errorCount++;
                    d("接收长度为负 errorCount ：" + errorCount);
                    if (errorCount > 10) {
                        break;
                    }
                }
            } catch (SocketException e) {
                printException(e);
                break;
            } catch (IOException e) {
                printException(e);
                break;
            }

        }
        stopException();
        d("isInterrupted");
    }

    public void stopException() {
        d("stop Exception");
        if (isForceStop) {
            return;
        }
        d("new Timer()");
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                d("new Thread()");
                if (listeners != null)
                    listeners.onRecvNetException();
            }
        }, 2000);

    }

    public void stopForce() {
        d("stop Force");
        isForceStop = true;
        stopRun();
    }

    public void stopRun() {
        d("TcpVideoReceive stopRun");
        listeners = null;
        close();
        interrupt();
    }

    private void close() {
        if (in != null) {
            try {
                d("in.close..");
                in.close();
            } catch (IOException e) {
                printException(e);
            }
        }
    }

    private void d(String msg) {
        Log.w(TAG, msg);
    }

    private void printException(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        LogUtil.d(TAG, "\n-------------------------------------------\n");
        LogUtil.d(TAG, writer.getBuffer().toString());
    }
}
