package com.minicreate.adas.transmission.listener;

import java.net.Socket;

public interface TcpConnectListener {
    public void onConnectStart(String server, int port);

    public void onConnectOk(Socket socket, String server, int port);

    public void onConnectOk();

    public void onConnectError(int code, String server, int port);
}
