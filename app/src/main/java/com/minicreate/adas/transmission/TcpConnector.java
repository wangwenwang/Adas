package com.minicreate.adas.transmission;

import android.util.Log;

import com.minicreate.adas.transmission.listener.TcpConnectListener;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;


public class TcpConnector extends Thread {
    private TcpConnectListener listener;
    private String ipAddr;
    private int port;
    private Socket connectedSock;
//	private int retries = 3;
    private ServerSocket serverSocket;
    private  Socket socket;

    public TcpConnector(int port, TcpConnectListener tcl) {

        this.port = port;
        setConnectionListener(tcl);
    }

    private void setConnectionListener(TcpConnectListener cl) {
        this.listener = cl;
    }

    @Override
    public void run() {
        d(Thread.currentThread().getId() + " run...");
        boolean error = false;

        try {
            serverSocket = new ServerSocket(port);
            d("serverSocket::"+serverSocket);
            if (serverSocket != null)
                socket = serverSocket.accept();
            d("client have connected!!!");
            notifyOnConnected(socket);
        } catch (BindException e) {
            d(e.getMessage());
            error = true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            d(e.getMessage());
            error = true;
        } catch (ConnectException e) {
            d(e.getMessage());
            error = true;
        } catch (SocketTimeoutException e) {
            d(e.getMessage());
            error = true;
        } catch (IOException e) {
            d(e.getMessage());
            error = true;
        } finally {
            if (error) {
                try {
                    d("error and close");
                    notifyOnConnectError(-1, "error", port);
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stopRun() {
        if(listener != null)
            listener = null;

        try {
            if(serverSocket != null){
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void notifyOnConnected(Socket socket) {
        if (listener != null)
            listener.onConnectOk(socket, "", port);
    }

    private void notifyOnConnectStart(String server, int port) {
        if (listener != null)
            listener.onConnectStart(server, port);
    }

    private void notifyOnConnectError(int code, String server, int port) {
        if (listener != null)
            listener.onConnectError(code, server, port);
    }

    private void d(String msg) {
        final String TAG = "TcpConnector";
        Log.w(TAG, msg);
    }

    public Socket getSocket(){
        return socket;
    }
}
