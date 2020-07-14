package com.minicreate.adas.transmission.listener;

import java.net.Socket;

/**
 * TcpConnectListener接口适配器
 */
public class TcpConnectListenerAdapter implements TcpConnectListener {
    @Override
    public void onConnectStart(String server, int port) {

    }

    @Override
    public void onConnectOk(Socket socket, String server, int port) {

    }

    @Override
    public void onConnectOk() {

    }

    @Override
    public void onConnectError(int code, String server, int port) {

    }
}
